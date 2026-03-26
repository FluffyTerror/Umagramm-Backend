package com.FluffyTerror.Umagramm_Backend.service.jwt;

import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = header.substring(7);

        return Mono.just(token)
                .flatMap(t -> {
                    try {
                        UUID userId = jwtService.extractUserId(t);
                        return userId != null ? Mono.just(userId.toString()) : Mono.empty();
                    } catch (Exception e) {
                        // Вместо Mono.empty() возвращаем ошибку, чтобы попасть в onErrorResume
                        return Mono.error(new RuntimeException("Invalid or expired token"));
                    }
                })
                .flatMap(userRepository::findById)
                .flatMap(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                // Если токен был, но мы не нашли юзера или userId был null
                .switchIfEmpty(Mono.error(new RuntimeException("User not found or token invalid")))
                .onErrorResume(e -> {
                    log.error("JWT Filter error: {}", e.getMessage());
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized: " + e.getMessage());
                });
    }

    // Вспомогательный метод для записи JSON (как в твоем Advice)
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Формируем JSON вручную (или через ObjectMapper)
        String json = String.format("{\"status\": %d, \"message\": \"%s\"}", status.value(), message);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

