package com.FluffyTerror.Umagramm_Backend.requests.qr;

import jakarta.validation.constraints.NotBlank;

public record QrRequest(
        @NotBlank(message = "инвайт токен не может быть пустым")
       String token
) {
}
