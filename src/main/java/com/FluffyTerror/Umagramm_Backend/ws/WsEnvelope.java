package com.FluffyTerror.Umagramm_Backend.ws;

public record WsEnvelope(
        WsMessageType type,
        String chatId,
        String payload
) {}
