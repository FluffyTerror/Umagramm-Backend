package com.FluffyTerror.Umagramm_Backend.service.mapper;

import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageMapper {

    public MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .chatId(message.getChatId())
                .type(message.getMessageType())
                .state(message.getMessageState())
                .createdAt(message.getCreatedDate())
                //--TODO считывание медиа
                .build();
    }
}
