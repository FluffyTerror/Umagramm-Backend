package com.FluffyTerror.Umagramm_Backend.service.mapper;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ChatMapper {

    private final ChatService chatService;

    public Mono<ChatResponse> toChatResponse(Chat chat, String currentUserId) {
        String recipientId = chat.getSenderId().equals(currentUserId)
                ? chat.getRecipientId()
                : chat.getSenderId();
        return Mono.zip(
                chatService.getChatName(chat.getId(), currentUserId),
                chatService.getUnreadMessages(chat.getId(), currentUserId),
                chatService.getLastMessage(chat.getId()),
                chatService.isRecipientOnline(recipientId)
        ).map(tuple -> {
            String name = tuple.getT1();
            Long unread = tuple.getT2();
            Message lastMsg = tuple.getT3();
            Boolean online = tuple.getT4();

            String content = null;
            LocalDateTime time = null;

            if (lastMsg.getId() != null) {
                time = lastMsg.getCreatedDate();
                content = (lastMsg.getMessageType() == MessageType.TEXT)
                        ? lastMsg.getContent()
                        : "Attachment";
            }

            return ChatResponse.builder()
                    .id(chat.getId())
                    .name(name)
                    .unreadCount(unread)
                    .lastMessage(content)
                    .lastMessageTime(time) // Убедись, что добавил это поле в ChatResponse
                    .isRecipientOnline(online)
                    .senderId(chat.getSenderId())
                    .receiverId(chat.getRecipientId())
                    .build();
        });
    }
}
