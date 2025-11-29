package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import org.springframework.data.annotation.Transient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Transient
    public String getChatName(final String senderId){
        Chat chat = c
        if (recipient.getId().equals(senderId)){
            return sender.getFirstName()+ " " + sender.getLastName();
        }
        return recipient.getFirstName() + " " + recipient.getLastName();
    }
}
