package com.FluffyTerror.Umagramm_Backend.service.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.exceptions.FileNotFoundException;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;


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

    public Mono<Resource> getAttachment(Long attachmentId) {
        String cacheKey = "attachment:" + attachmentId;

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .map(bytes -> (Resource) new ByteArrayResource(bytes))
                .switchIfEmpty(readFromDiskAndCache(attachmentId, cacheKey));
    }

    private Mono<Resource> readFromDiskAndCache(Long attachmentId, String cacheKey) {
        return attachmentRepository.findById(attachmentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(attachment -> {

                    Path path = Paths.get(attachment.getUrl())
                            .toAbsolutePath()
                            .normalize();

                    return Mono.fromCallable(() -> {
                                if (!Files.exists(path)) {
                                    throw new FileNotFoundException("File not found");
                                }
                                if (!Files.isReadable(path)) {
                                    throw new AccessDeniedException("Cannot read file");
                                }
                                return path;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(validPath -> {

                                return Mono.fromCallable(() -> Files.size(validPath))
                                        .flatMap(size -> {

                                            // если мелочь ебашим в кеш
                                            if (size > 0 && size <= MAX_CACHE_SIZE) {
                                                return Mono.fromCallable(() -> Files.readAllBytes(validPath))
                                                        .subscribeOn(Schedulers.boundedElastic())
                                                        .flatMap(bytes ->
                                                                redisTemplate.opsForValue()
                                                                        .set(cacheKey, bytes, Duration.ofHours(1))
                                                                        .thenReturn((Resource) new ByteArrayResource(bytes))
                                                        );
                                            }


                                            return Mono.just((Resource) new FileSystemResource(validPath));//стриминг с диска
                                        });
                            });
                });
    }
}