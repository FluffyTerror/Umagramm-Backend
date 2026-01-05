package com.FluffyTerror.Umagramm_Backend.controller.message;

import com.FluffyTerror.Umagramm_Backend.requests.message.MessageRequest;
import com.FluffyTerror.Umagramm_Backend.responses.message.MessageResponse;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/messages")
@Tag(name = "Message")
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> saveMessage(@RequestBody MessageRequest request
            , Authentication authentication) {
        return messageService.saveMessage(request, authentication);
    }

    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> upload(
            @RequestPart("file") FilePart file,
            @Parameter()
            @RequestPart("chatId") String chatId,
            Authentication authentication
    ) {
        return messageService.uploadMediaMessage(chatId, file, authentication);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> setMessagesToSeen(@RequestParam("chat-id") String chatId, Authentication authentication) {
        return messageService.setMessagesToSeen(chatId, authentication);
    }

    @GetMapping("chat/{chat-id}")
    public Mono<ResponseEntity<List<MessageResponse>>> getMessages(@PathVariable("chat-id") String chatId) {
        return messageService.findMessages(chatId)
                .map(ResponseEntity::ok);
    }
}
