package com.FluffyTerror.Umagramm_Backend.requests.userInfo;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String username
) {}
