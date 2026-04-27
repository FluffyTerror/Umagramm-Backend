package com.FluffyTerror.Umagramm_Backend.controller.chat;

import com.FluffyTerror.Umagramm_Backend.models.chatMember.ChatMember;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.requests.AddChatMemberRequest;
import com.FluffyTerror.Umagramm_Backend.requests.CreateGroupChatRequest;
import com.FluffyTerror.Umagramm_Backend.requests.KickChatMemberRequest;
import com.FluffyTerror.Umagramm_Backend.responses.ChatMemberResponse;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
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
     */
    @PostMapping("/private")
    public Mono<ChatResponse> createPrivateChat(@RequestParam(name = "receiver-id") String receiverId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return chatService.createPrivateChat(user.getId(), receiverId);
    }

    @PostMapping(value = "/group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ChatResponse> createGroup(
            @RequestPart("data") CreateGroupChatRequest request,
            @RequestPart("file") Mono<FilePart> filePart,
            @AuthenticationPrincipal User user
    ) {
        return chatService.createGroupChat(user.getId(), request, filePart);
    }

    @PostMapping("/add-chat-member")
    public Mono<ChatMember> addChatMember(AddChatMemberRequest req,@AuthenticationPrincipal User user){
        return chatService.addChatMemberToChat(req, user.getId());
    }

    @PostMapping("/kick-chat-member")
    public Mono<Void> kickChatMember(KickChatMemberRequest req, @AuthenticationPrincipal User user){
        return chatService.kickChatMember(req, user.getId());
    }

    @GetMapping("/{chatId}/members")
    public Mono<List<ChatMemberResponse>> getChatMembers(
            @PathVariable String chatId,
            @AuthenticationPrincipal User user) {
        return chatService.getChatMembers(chatId, user.getId());
    }

    /**
     * Получение всех чатов текущего пользователя (и личных, и групп).
     */
    @GetMapping("/by-user")
    public Mono<ResponseEntity<List<ChatResponse>>> getUserChats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return chatService.getUserChats(user.getId()).collectList().map(ResponseEntity::ok);
    }
}
