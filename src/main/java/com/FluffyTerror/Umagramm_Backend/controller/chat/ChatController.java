package com.FluffyTerror.Umagramm_Backend.controller.chat;

import com.FluffyTerror.Umagramm_Backend.responses.StringResponse;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/chats")
@Tag(name = "Chat")
public class ChatController {
    private final ChatService chatService;


    @PostMapping
    public Mono<ResponseEntity<StringResponse>> createChat(
            @RequestParam(name = "sender-id") String senderId,
            @RequestParam(name = "receiver-id") String receiverId
    ) {
        return chatService.createChat(senderId, receiverId).map(
                chatId -> ResponseEntity.ok(StringResponse
                        .builder()
                        .response(chatId)
                        .build())
        );
    }

    @GetMapping
    public Mono<ResponseEntity<List<ChatResponse>>> getChatsByReceiver(Authentication authentication) {
        return chatService.getChatByReceiverId(authentication).map(
                        ResponseEntity::ok);
    }

}
