package com.FluffyTerror.Umagramm_Backend.controller.chat;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
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

    /**
     * Создание личного чата.
     * Теперь нам нужен только ID того, с кем мы хотим начать чат.
     * Наш собственный ID берется из Authentication.
     */
    @PostMapping("/private")
    public Mono<ChatResponse> createPrivateChat(
            @RequestParam(name = "receiver-id") String receiverId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return chatService.createPrivateChat(user.getId(), receiverId);
    }

    /**
     * Получение всех чатов текущего пользователя (и личных, и групп).
     */
//    @GetMapping("/by-user")
//    public Mono<ResponseEntity<List<ChatResponse>>> getUserChats(Authentication authentication) {
//        String currentUserId = authentication.getName();
//        return chatService.getUserChats(currentUserId)
//                .collectList() // Превращаем Flux в Mono<List>
//                .map(ResponseEntity::ok);
//    }
}
