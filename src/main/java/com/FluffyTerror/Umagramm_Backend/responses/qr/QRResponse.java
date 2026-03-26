package com.FluffyTerror.Umagramm_Backend.responses.qr;

public record QRResponse(
        String token     // нужна для подбора ключа к recovery коду
) {
}
