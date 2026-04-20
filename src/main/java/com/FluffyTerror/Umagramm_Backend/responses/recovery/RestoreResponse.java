package com.FluffyTerror.Umagramm_Backend.responses.recovery;

public record RestoreResponse(
        String accessToken,
        String refreshToken,
        String encryptedPrivateKey,
        String salt,
        String nonce// нужна для подбора ключа к recovery коду
) {
}
