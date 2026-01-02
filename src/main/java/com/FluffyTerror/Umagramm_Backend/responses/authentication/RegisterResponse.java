package com.FluffyTerror.Umagramm_Backend.responses.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;

public record RegisterResponse(
        String refreshToken,
        String accessToken,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String recoveryCode
) {
}
