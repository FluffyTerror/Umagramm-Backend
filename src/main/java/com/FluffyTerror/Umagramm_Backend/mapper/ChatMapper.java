package com.FluffyTerror.Umagramm_Backend.mapper;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ChatMapper {


    public ChatResponse toChatResponse(Chat chat,
                                       String chatName,
                                       Long unreadCount,
                                       Message lastMessage,
                                       Boolean isRecipientOnline) {

        String content = null;
        LocalDateTime time = null;


        if (lastMessage != null && lastMessage.getId() != null) {
            time = lastMessage.getCreatedDate();
            content = (lastMessage.getMessageType() == MessageType.TEXT)
                    ? lastMessage.getContent()
                    : "Attachment";
        }

        return ChatResponse.builder()
                .id(chat.getId())
                .name(chat.getTitle())
                .unreadCount(unreadCount)
                .lastMessage(content)
                .lastMessageTime(time)
                .isRecipientOnline(isRecipientOnline)
                .senderId(chat.getSenderId())
                .receiverId(chat.getRecipientId())
                .build();
    }
}
