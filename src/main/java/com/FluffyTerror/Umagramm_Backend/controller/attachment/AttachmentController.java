package com.FluffyTerror.Umagramm_Backend.controller.attachment;

import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{attachment-id}")
    public Mono<ResponseEntity<Resource>> viewAttachment(@PathVariable("attachment-id") Long attachmentId) {
        return attachmentService.getAttachment(attachmentId)
                .map(resource -> {
                    MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM);

                    String contentDisposition = getContentDisposition(resource, mediaType);//чекаем на тип медиа контента

                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                            .body(resource);
                });
    }

    private static String getContentDisposition(Resource resource, MediaType mediaType) {
        boolean isMedia = mediaType.getType().equals("IMAGE") ||
                mediaType.getType().equals("VIDEO") ||
                mediaType.getType().equals("AUDIO");
        String dispositionType = isMedia ? "inline" : "attachment";
        String filename = resource.getFilename() != null ? resource.getFilename() : "file";
        return dispositionType + "; filename=\"" + filename + "\"";
    }

}
