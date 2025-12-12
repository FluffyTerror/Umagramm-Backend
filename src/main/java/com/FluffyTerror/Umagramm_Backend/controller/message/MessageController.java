package com.FluffyTerror.Umagramm_Backend.controller.message;

import com.FluffyTerror.Umagramm_Backend.service.message.MessageRequest;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageResponse;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/messages")
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> saveMessage(@RequestBody MessageRequest request) {
        return messageService.saveMessage(request);
    }

    @PostMapping(value = "/upload-media", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> uploadMedia(
            @RequestParam("chat-id") String chatId,
            //--TODO add @Parameter from swagger
            @RequestParam("file") MultipartFile file,
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
