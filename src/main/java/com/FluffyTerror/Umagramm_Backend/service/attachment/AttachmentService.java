package com.FluffyTerror.Umagramm_Backend.service.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.mapper.MessageMapper;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;


import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.requests.media.UploadInitRequest;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.ResourceData;
import com.FluffyTerror.Umagramm_Backend.responses.media.UploadInitResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;
    private final String fileUploadPath;
    private final MessageMapper mapper;
    private static final long MAX_CACHE_SIZE = 5 * 1024 * 1024;

    public AttachmentService(
            AttachmentRepository attachmentRepository, ChatMemberRepository chatMemberRepository,
            ReactiveRedisTemplate<String, byte[]> redisTemplate,
            @Value("${application.file.uploads.media-output-path}") String fileUploadPath, MessageMapper mapper
    ) {
        this.attachmentRepository = attachmentRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.redisTemplate = redisTemplate;
        this.fileUploadPath = fileUploadPath;
        this.mapper = mapper;
    }
    public Mono<Attachment> saveGroupAvatar(FilePart filePart, String senderId) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        // Определяем путь
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
                .flatMap(filePart::transferTo) // Сохраняем файл
                .then(Mono.defer(() -> {
                    // Создаем сущность для БД
                    Attachment attachment = new Attachment();
                    attachment.setFilename(filename);
                    attachment.setUrl(target.toString());
                    attachment.setMessageId(null);
                    attachment.setContentType(filePart.headers().getContentType().toString());
                    attachment.setCreatedAt(Instant.now());
                     attachment.setType(MessageType.IMAGE);

                    return attachmentRepository.save(attachment); // Сохраняем в БД и возвращаем сущность с ID
                }));
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

    public Mono<List<AttachmentResponse>> getAttachmentsByChat(String chatId, String userId) {
        chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .switchIfEmpty(Mono.error(new AccessDeniedException("You are not a member of this chat")));
        
        return attachmentRepository.findAttachmentsByChat(chatId)
                .map(mapper::toAttachmentResponse)
                .collectList()
                .defaultIfEmpty(List.of()); // Возвращаем пустой список, если вложений нет
    }

    //--TODO переделать загрузку медиа
    public Mono<UploadInitResponse> initUploadSession(UploadInitRequest request, String userId) {
        String uploadId = UUID.randomUUID().toString();
        String redisKey = "upload_session:" + uploadId;

        // Сохраняем в Redis метаданные: кто грузит, имя файла, сколько всего чанков ждем
        Map<String, String> meta = Map.of(
                "userId", userId,
                "filename", request.getFilename(),
                "totalChunks", String.valueOf(request.getTotalChunks())
        );

        return redisTemplate.opsForHash().putAll(redisKey, meta)
                .then(redisTemplate.expire(redisKey, Duration.ofDays(1))) // Сессия живет сутки
                .thenReturn(new UploadInitResponse(uploadId));
    }
    public Mono<Void> saveChunk(String uploadId, int chunkIndex, FilePart filePart) {
        Path tmpFolder = Paths.get(fileUploadPath, "tmp", uploadId);
        Path chunkPath = tmpFolder.resolve("chunk_" + chunkIndex);

        return Mono.fromCallable(() -> {
                    if (!Files.exists(tmpFolder)) {
                        Files.createDirectories(tmpFolder);
                    }
                    return chunkPath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(filePart::transferTo);
    }
    public Mono<Void> mergeChunksAndSave(String uploadId, String chatId, Authentication auth) {
        String redisKey = "upload_session:" + uploadId;

        // 1. Достаем метаданные из Redis
        return redisTemplate.opsForHash().entries(redisKey).collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(meta -> {
                    String userId = (String) meta.get("userId");
                    String filename = System.currentTimeMillis() + "_" + meta.get("filename");
                    int totalChunks = Integer.parseInt((String) meta.get("totalChunks"));

                    Path tmpFolder = Paths.get(fileUploadPath, "tmp", uploadId);
                    Path finalDest = Paths.get(fileUploadPath, "users", userId, filename);

                    return Mono.fromCallable(() -> {
                        if (!Files.exists(finalDest.getParent())) {
                            Files.createDirectories(finalDest.getParent());
                        }

                        // 2. Склеиваем файлы через FileChannel (очень быстро и без расхода RAM)
                        try (FileChannel destChannel = FileChannel.open(finalDest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                            for (int i = 0; i < totalChunks; i++) {
                                Path chunk = tmpFolder.resolve("chunk_" + i);
                                try (FileChannel srcChannel = FileChannel.open(chunk, StandardOpenOption.READ)) {
                                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
                                }
                                Files.deleteIfExists(chunk); // Удаляем кусок после склейки
                            }
                        }
                        Files.deleteIfExists(tmpFolder); // Удаляем временную папку

                        return finalDest;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(finalPath -> {
                    // 3. Файл склеен! Теперь создаем сущность Attachment в БД
                    // и вызываем твой старый добрый messageService.uploadMediaMessage

                    // redisTemplate.delete(redisKey).subscribe(); // Чистим сессию
                    return Mono.empty(); // Заглушка, тут вызовешь сохранение в базу
                });
    }
}