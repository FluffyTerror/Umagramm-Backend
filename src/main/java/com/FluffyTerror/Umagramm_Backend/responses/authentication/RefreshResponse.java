package com.FluffyTerror.Umagramm_Backend.responses.authentication;

import java.time.LocalDateTime;

public record RefreshResponse(
        String accessToken,
        LocalDateTime expiresAt
) {
}
