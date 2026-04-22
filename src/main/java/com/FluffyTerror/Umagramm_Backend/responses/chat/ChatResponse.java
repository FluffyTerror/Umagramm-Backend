package com.FluffyTerror.Umagramm_Backend.responses.chat;



import java.time.LocalDateTime;

public record ChatResponse (
         String id,
         String name,
         String senderId,
         String receiverId,
         String type,
         Long unreadCount,
         String lastMessage,
         LocalDateTime lastMessageTime,
         boolean isRecipientOnline,
         Long avatarId

){}
