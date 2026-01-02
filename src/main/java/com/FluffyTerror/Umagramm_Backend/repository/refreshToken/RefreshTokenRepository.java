package com.FluffyTerror.Umagramm_Backend.repository.refreshToken;

import com.FluffyTerror.Umagramm_Backend.models.refreshToken.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {

    Mono<RefreshToken> findByTokenAndRevokedFalse(String token);
}
