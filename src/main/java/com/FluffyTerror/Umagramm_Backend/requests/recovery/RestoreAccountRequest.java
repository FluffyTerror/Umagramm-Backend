package com.FluffyTerror.Umagramm_Backend.requests.recovery;

import jakarta.validation.constraints.NotBlank;

public record RestoreAccountRequest(
        @NotBlank
        String username,
        @NotBlank
        String recoveryProof //код из 16 символов
) {
}
