package com.FluffyTerror.Umagramm_Backend.requests.groupChat;

import java.util.List;

public record CreateGroupChatRequest(
        List<String> usersId,
        String title
) {
}
