package com.FluffyTerror.Umagramm_Backend.service.jwt;

import com.FluffyTerror.Umagramm_Backend.models.refreshToken.RefreshToken;
import com.FluffyTerror.Umagramm_Backend.repository.refreshToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Value("${application.security.jwt.refresh-token-ttl}")
    private Duration refreshTtlDays;

    public Mono<RefreshToken> create(UUID userId) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(Instant.now().plus(refreshTtlDays));
        rt.setRevoked(false);

        return repo.save(rt);
    }

    public Mono<RefreshToken> validate(String token) {
        return repo.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()))
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")));
    }

    public Mono<RefreshToken> rotate(RefreshToken oldToken) {
        oldToken.setRevoked(true);

        return repo.save(oldToken)
                .then(create(oldToken.getUserId()));
    }

    public Mono<Void> revokeAll(UUID userId) {
        return repo.findAll()
                .filter(t -> t.getUserId().equals(userId))
                .flatMap(t -> {
                    t.setRevoked(true);
                    return repo.save(t);
                })
                .then();
    }
}

