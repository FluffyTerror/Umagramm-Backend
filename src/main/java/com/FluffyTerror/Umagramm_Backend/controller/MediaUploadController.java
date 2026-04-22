package com.FluffyTerror.Umagramm_Backend.controller;

import com.FluffyTerror.Umagramm_Backend.requests.media.FinishUploadRequest;
import com.FluffyTerror.Umagramm_Backend.requests.media.UploadInitRequest;
import com.FluffyTerror.Umagramm_Backend.responses.media.UploadInitResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private final AttachmentService attachmentService;

    // 1. Инициация загрузки
    @PostMapping("/init")
    public Mono<UploadInitResponse> initUpload(
            @RequestBody UploadInitRequest request,
            Authentication authentication) {
        // Возвращает уникальный uploadId (сессию)
        return attachmentService.initUploadSession(request, authentication.getName());
    }

    // 2. Загрузка куска (чанка)
    @PutMapping(value = "/chunk/{uploadId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> uploadChunk(
            @PathVariable String uploadId,
            @RequestPart("chunkIndex") String chunkIndex, // Номер куска (0, 1, 2...)
            @RequestPart("file") FilePart filePart) {
        return attachmentService.saveChunk(uploadId, Integer.parseInt(chunkIndex), filePart);
    }

    // 3. Завершение и склейка
    @PostMapping("/finish/{uploadId}")
    public Mono<Void> finishUpload(
            @PathVariable String uploadId,
            @RequestBody FinishUploadRequest request,
            Authentication authentication) {
        // Здесь происходит склейка файлов на диске и вызов messageService.uploadMediaMessage
        return attachmentService.mergeChunksAndSave(uploadId, request.getChatId(), authentication);
    }
}
