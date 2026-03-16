package com.FluffyTerror.Umagramm_Backend.responses.recovery;

public record RestoreResponse(
        String accessToken,
        String refreshToken,
        String encryptedPrivateKey,
        String salt                 // нужна для подбора ключа к recovery коду
) {
}
