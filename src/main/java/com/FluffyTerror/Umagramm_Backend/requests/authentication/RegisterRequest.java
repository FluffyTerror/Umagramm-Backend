package com.FluffyTerror.Umagramm_Backend.requests.authentication;


public record RegisterRequest(
        String username,
        String firstName,
        String lastName

        //String encryptedMessagingKey
) {
}

