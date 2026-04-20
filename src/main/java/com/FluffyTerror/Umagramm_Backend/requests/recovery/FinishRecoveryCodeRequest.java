package com.FluffyTerror.Umagramm_Backend.requests.recovery;

import jakarta.validation.constraints.NotBlank;

public record FinishRecoveryCodeRequest(
        @NotBlank(message = "Зашифрованный ключ не может быть пустым")
        String encryptedPrivateKey,
        @NotBlank(message = "Соль не может быть пустой")
        String salt,
        @NotBlank(message = "Вектор инициализации не может быть пустым")
        String nonce
) {
}
