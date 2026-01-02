package com.FluffyTerror.Umagramm_Backend.requests.authentication;

import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;

public record RegisterRequest(
        String username,
        String firstName,
        String lastName

        //String encryptedMessagingKey
) {
}

