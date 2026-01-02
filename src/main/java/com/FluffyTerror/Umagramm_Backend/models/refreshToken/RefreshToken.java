package com.FluffyTerror.Umagramm_Backend.models.refreshToken;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("refresh_token")
@Data
public class RefreshToken {

    @Id
    private UUID id;

    private UUID userId;

    private String token;

    private Instant expiresAt;

    private boolean revoked;
}