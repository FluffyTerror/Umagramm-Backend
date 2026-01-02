package com.FluffyTerror.Umagramm_Backend.requests.authentication;

public record RecoverRequest(
        String username,
        String recoveryCode
) {
}
