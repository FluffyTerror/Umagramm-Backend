package com.FluffyTerror.Umagramm_Backend.requests;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String username
) {}
