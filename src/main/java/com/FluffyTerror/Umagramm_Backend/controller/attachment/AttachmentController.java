package com.FluffyTerror.Umagramm_Backend.controller.attachment;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{attachment-id}")
    public Mono<ResponseEntity<Resource>> viewAttachment(@PathVariable("attachment-id") Long attachmentId, @AuthenticationPrincipal User user) {
        return attachmentService.getAttachment(attachmentId)
                .flatMap(resourceData ->
                        Mono.fromCallable(() -> {
                            Resource resource = resourceData.getResource();

                            if (!resource.exists()) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
                            }

                            if (!resource.isReadable()) {
                                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File not readable");
                            }

                            MediaType mediaType;
                            try {
                                mediaType = MediaType.parseMediaType(resourceData.getContentType());
                            } catch (Exception e) {
                                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                            }

                            long length = resource.contentLength();

                            return ResponseEntity.ok()
                                    .contentType(mediaType)
                                    //.contentLength(length)
                                    .header(HttpHeaders.CONTENT_DISPOSITION,
                                            getContentDisposition(resourceData.getFilename(), mediaType))
                                    .body(resource);
                        }).subscribeOn(Schedulers.boundedElastic())
                );
    }
    @GetMapping("/chat/{chatId}")
    public Mono<List<AttachmentResponse>> getAttachmentsByChat(@PathVariable("chatId") String chatId, @AuthenticationPrincipal User user){
        return attachmentService.getAttachmentsByChat(chatId,user.getId());
    }

    private static String getContentDisposition(String filename, MediaType mediaType) {
        String majorType = mediaType.getType().toLowerCase();

        boolean isMedia = majorType.equals("image") ||
                majorType.equals("video") ||
                majorType.equals("audio");

        String dispositionType = isMedia ? "inline" : "attachment";
        String safeFilename = filename != null ? filename : "file";

        return dispositionType + "; filename=\"" + safeFilename + "\"";
    }

}
