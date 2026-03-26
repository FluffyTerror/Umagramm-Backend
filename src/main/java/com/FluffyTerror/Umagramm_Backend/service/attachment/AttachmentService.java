package com.FluffyTerror.Umagramm_Backend.service.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.exceptions.FileNotFoundException;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource; // Если решишь использовать URL вместо путей
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@Service
public class AttachmentService {
    public AttachmentService(
            AttachmentRepository attachmentRepository,
            @Value("${application.file.uploads.media-output-path}") String fileUploadPath
    ) {
        this.attachmentRepository = attachmentRepository;
        this.fileUploadPath = fileUploadPath;
    }

    private final AttachmentRepository attachmentRepository;
    @Value("${application.file.uploads.media-output-path}")
    private String fileUploadPath;

    //final String fileUploadSubPath = "users" + File.separator + senderId;
    public Mono<String> saveFile(FilePart filePart, String senderId) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        Path target = Paths.get(fileUploadPath, "users", senderId, filename);

        return Mono.fromCallable(() -> Files.createDirectories(target.getParent()))
                .then(filePart.transferTo(target))
                .thenReturn(target.toString());
    }

    public Mono<Resource> getAttachment(Long attachmentId){
        return attachmentRepository.findById(attachmentId).switchIfEmpty(Mono.error(new RuntimeException("Attachment not found")))
                .map(attachment -> {
                    // Путь к файлу, который мы сохраняли ранее (например, "uploads/abc.jpg")
                    Path path = Paths.get(attachment.getUrl());

                    if (!Files.exists(path)) {
                        throw new FileNotFoundException("File not found on disk: " + attachment.getUrl());
                    }

                    return new FileSystemResource(path);
                });


    }
    private Mono<String> uploadFile(FilePart filePart, String subPath) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        Path target = Paths.get(fileUploadPath, subPath, filename);

        return Mono.fromCallable(() -> Files.createDirectories(target.getParent()))
                .then(filePart.transferTo(target))
                .doOnSuccess(v -> log.info("Saved file to {}", target))
                .thenReturn(target.toString());
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "";
        }
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }
}
