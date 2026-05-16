package com.FluffyTerror.Umagramm_Backend.requests.groupChat;

public record KickChatMemberRequest(
        String userId,
        String chatId

) {
}
