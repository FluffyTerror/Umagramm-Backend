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
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
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

import java.io.File;
import java.io.InputStream;
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
    private final MinioClient minioClient;

    @Value("${application.minio.bucket-name}")
    String bucketName;

    public AttachmentService(
            AttachmentRepository attachmentRepository, ChatMemberRepository chatMemberRepository,
            ReactiveRedisTemplate<String, byte[]> redisTemplate,
            @Value("${application.file.uploads.media-output-path}") String fileUploadPath, MessageMapper mapper, MinioClient minioClient
    ) {
        this.attachmentRepository = attachmentRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.redisTemplate = redisTemplate;
        this.fileUploadPath = fileUploadPath;
        this.mapper = mapper;
        this.minioClient = minioClient;
    }
    public Mono<Attachment> saveGroupAvatar(FilePart filePart, String senderId) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        return Mono.fromCallable(() -> {
                    // Создаем временный файл в контейнере, чтобы перелить туда FilePart
                    Path tempFile = Files.createTempFile("upload_","_"+ filename);
                    return tempFile;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tempFile->filePart.transferTo(tempFile)// Асинхронно сохраняем во временный файл
                                .then(Mono.fromCallable(()->{
                    // Блокирующий вызов MinIO оборачиваем в воркер-поток
                    try (InputStream is = Files.newInputStream(tempFile)){
                        minioClient.putObject(
                                PutObjectArgs.builder().
                                        bucket(bucketName).
                                        object("avatars/"+senderId+"/"+filename)
                                        .stream(is, Files.size(tempFile),-1L)
                                        .contentType(filePart.headers().getContentType().toString())
                                        .build());
                    }finally {
                        Files.deleteIfExists(tempFile);
                    }

                    //  В URL базы теперь пишем КЛЮЧ объекта в MinIO, а не путь к диску!
                    Attachment attachment = new Attachment();
                    attachment.setFilename(filename);
                    attachment.setUrl("avatars/" + senderId + "/" + filename);
                    attachment.setMessageId(null);
                    attachment.setContentType(filePart.headers().getContentType().toString());
                    attachment.setCreatedAt(Instant.now());
                    attachment.setType(MessageType.IMAGE);

                    return attachment;
                }).subscribeOn(Schedulers.boundedElastic()))
                ).flatMap(attachmentRepository::save);

    }
    public Mono<String> saveFile(FilePart filePart, String senderId) {
        // Формируем имя файла и итоговый путь (ключ) в бакете
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        String objectKey = "avatars/" + senderId + "/" + filename;

        return Mono.fromCallable(() -> Files.createTempFile("upload_", "_" + filename))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tempFile ->
                        filePart.transferTo(tempFile) // Асинхронно сохраняем FilePart на диск контейнера
                                .then(Mono.fromCallable(() -> {
                                    // Блокирующий код прячем в Callable
                                    try (InputStream is = Files.newInputStream(tempFile)) {
                                        minioClient.putObject(
                                                PutObjectArgs.builder()
                                                        .bucket(bucketName) // Убедись, что переменная bucketName объявлена в сервисе!
                                                        .object(objectKey)
                                                        .stream(is, Files.size(tempFile), -1L)
                                                        .contentType(filePart.headers().getContentType().toString())
                                                        .build()
                                        );

                                        // ВАЖНО: Возвращаем ключ объекта, чтобы он улетел в метод register
                                        return objectKey;
                                    } finally {
                                        // Обязательно удаляем временный файл при любом исходе (успех или ошибка)
                                        Files.deleteIfExists(tempFile);
                                    }
                                }).subscribeOn(Schedulers.boundedElastic())) // Этот Callable тоже пускаем в пуле
                );
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
                                .switchIfEmpty(readFromMinioAndCache(attachment, cacheKey))
                );
    }

    private Mono<ResourceData> readFromMinioAndCache(Attachment attachment, String cacheKey) {

        return Mono.fromCallable(() -> {
                    return minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(attachment.getUrl())
                                    .build()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(inputStream -> Mono.fromCallable(() -> {
                    try (inputStream) {
                        byte[] bytes = inputStream.readAllBytes();

                        // Если размер подходит под кэш — сохраняем в Redis
                        if (bytes.length <= MAX_CACHE_SIZE) {
                            redisTemplate.opsForValue()
                                    .set(cacheKey, bytes, Duration.ofHours(1))
                                    .subscribe(); // Асинхронно пушим в редис
                        }

                        return new ResourceData(
                                attachment.getFilename(),
                                attachment.getContentType(),
                                new ByteArrayResource(bytes)
                        );
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<List<AttachmentResponse>> getAttachmentsByChat(String chatId, String userId) {
        chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .switchIfEmpty(Mono.error(new AccessDeniedException("You are not a member of this chat")));
        
        return attachmentRepository.findAttachmentsByChat(chatId)
                .map(mapper::toAttachmentResponse)
                .collectList()
                .defaultIfEmpty(List.of()); // Возвращаем пустой список, если вложений нет
    }
}