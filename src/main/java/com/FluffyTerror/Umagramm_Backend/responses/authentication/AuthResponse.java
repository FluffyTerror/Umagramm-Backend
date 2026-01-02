package com.FluffyTerror.Umagramm_Backend.responses.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AuthResponse(
        String refreshToken,
        String accessToken,
        String issuer
) {
}
