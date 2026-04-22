package com.FluffyTerror.Umagramm_Backend.requests;

import com.FluffyTerror.Umagramm_Backend.models.user.User;

import java.util.List;

public record CreateGroupChatRequest(
        List<String> usersId,
        String title
) {
}
