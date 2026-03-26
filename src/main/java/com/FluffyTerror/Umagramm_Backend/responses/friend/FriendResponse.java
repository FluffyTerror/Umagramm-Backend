package com.FluffyTerror.Umagramm_Backend.responses.friend;

import java.time.LocalDateTime;


public record FriendResponse(
        String id,
        String username,
         LocalDateTime createdAt) {
}
