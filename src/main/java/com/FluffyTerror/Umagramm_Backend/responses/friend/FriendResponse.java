package com.FluffyTerror.Umagramm_Backend.responses.friend;

import java.time.LocalDateTime;


public record FriendResponse(
       String id,         // ID друга как пользователя
       String username,
       String firstName,
       String lastName,
       Long photoId,      // Тот самый ID для Glide/Coil на Android
       LocalDateTime createdAt
) {
}
