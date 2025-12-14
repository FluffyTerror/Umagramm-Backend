package com.FluffyTerror.Umagramm_Backend.service.attachment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {
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

    //    private String uploadFile(@NonNull MultipartFile sourceFile,@NonNull String fileUploadSubPath) {
//        final String finalUploadPath = fileUploadPath + File.separator + fileUploadSubPath;
//        File targetFolder = new File(finalUploadPath);
//
//        if(!targetFolder.exists()){
//            boolean folderCreated = targetFolder.mkdirs();
//            if (!folderCreated){
//                log.warn("failed to create target folder, {}",targetFolder);
//                return null;
//            }
//        }
//        final String fileExtension = getFileExtension(sourceFile.getOriginalFilename());
//        String targetFilePath = finalUploadPath + File.separator + currentTimeMillis() + fileExtension;
//        Path targetPath = Paths.get(targetFilePath);
//        try{
//            Files.write(targetPath,sourceFile.getBytes());
//            log.info("File saved to {}",targetPath);
//            return targetFilePath;
//        } catch (IOException e) {
//            log.error("Error saving a file with extension {}", fileExtension);
//            log.error("Failed to save file",e);
//        }
//        return null;
//    }
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
