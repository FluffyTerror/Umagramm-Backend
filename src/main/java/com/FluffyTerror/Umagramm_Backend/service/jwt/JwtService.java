package com.FluffyTerror.Umagramm_Backend.service.jwt;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Service
public class JwtService {
    @Value("${application.security.jwt.secret}")
    private String secret;

    @Value("${application.security.jwt.access-token-ttl}")
    private Duration accessTtl;

    private SecretKey key() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .subject(user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key())
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
