package com.FluffyTerror.Umagramm_Backend.service.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.exceptions.FileNotFoundException;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;


import com.FluffyTerror.Umagramm_Backend.responses.attachment.ResourceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource; // Если решишь использовать URL вместо путей
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;


@Slf4j
@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;
    private final String fileUploadPath;

    private static final long MAX_CACHE_SIZE = 5 * 1024 * 1024;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            ReactiveRedisTemplate<String, byte[]> redisTemplate,
            @Value("${application.file.uploads.media-output-path}") String fileUploadPath
    ) {
        this.attachmentRepository = attachmentRepository;
        this.redisTemplate = redisTemplate;
        this.fileUploadPath = fileUploadPath;
    }

    public Mono<String> saveFile(FilePart filePart, String senderId) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        Path target = Paths.get(fileUploadPath, "users", senderId, filename)
                .toAbsolutePath()
                .normalize();

        return Mono.fromCallable(() -> {
                    if (!Files.exists(target.getParent())) {
                        Files.createDirectories(target.getParent());
                    }
                    return target;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(path -> filePart.transferTo(path))
                .thenReturn(target.toString());
    }

    public Mono<ResourceData> getAttachment(Long attachmentId) {
        String cacheKey = "attachment:" + attachmentId;

        // Сначала получаем метаданные из БД
        return attachmentRepository.findById(attachmentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found in DB")))
                .flatMap(attachment ->
                        // Ищем байты в кэше
                        redisTemplate.opsForValue().get(cacheKey)
                                // Если нашли в кэше, собираем ResourceData с ByteArrayResource
                                .map(bytes -> new ResourceData(
                                        attachment.getFilename(),
                                        attachment.getContentType(),
                                        new ByteArrayResource(bytes)
                                ))
                                // Если в кэше пусто, идем на диск
                                .switchIfEmpty(readFromDiskAndCache(attachment, cacheKey))
                );
    }

    private Mono<ResourceData> readFromDiskAndCache(Attachment attachment, String cacheKey) {
        Path path = Paths.get(attachment.getUrl());

        return Mono.fromCallable(() -> {
                    if (!Files.exists(path)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
                    }
                    return Files.size(path);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(size -> {
                    // Если файл небольшой — читаем в память и кэшируем
                    if (size > 0 && size <= MAX_CACHE_SIZE) {
                        return Mono.fromCallable(() -> Files.readAllBytes(path))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(bytes -> redisTemplate.opsForValue()
                                        .set(cacheKey, bytes, Duration.ofHours(1))
                                        .thenReturn(new ResourceData(
                                                attachment.getFilename(),
                                                attachment.getContentType(),
                                                new ByteArrayResource(bytes)
                                        ))
                                );
                    }

                    // Стриминг с диска для больших файлов
                    return Mono.just(new ResourceData(
                            attachment.getFilename(),
                            attachment.getContentType(),
                            new FileSystemResource(path)
                    ));
                });
    }
}