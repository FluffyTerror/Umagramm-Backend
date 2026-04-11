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
        String path = exchange.getRequest().getURI().getPath();
        if (path.contains("/auth/register") || path.contains("/auth/login")) {
            return chain.filter(exchange);
        }
        // 1. Логируем путь и наличие заголовка вообще
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("Request Path: {} | Auth Header: {}", path, header);

        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = header.substring(7);
        return Mono.just(token)
                .flatMap(t -> {
                    try {
                        UUID userId = jwtService.extractUserId(t);
                        return userId != null ? Mono.just(userId.toString()) : Mono.error(new RuntimeException("Token invalid"));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Invalid or expired token"));
                    }
                })
                .flatMap(userId -> userRepository.findById(userId)
                        .switchIfEmpty(Mono.error(new RuntimeException("User not found in DB: " + userId))) // Теперь switch тут!
                )
                .flatMap(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                // УБРАЛИ switchIfEmpty отсюда
                .onErrorResume(e -> {
                    // Если контроллер уже начал отвечать (например, 404), не пытаемся писать 401
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.error(e);
                    }
                    log.error("JWT Filter error: {}", e.getMessage());
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
                });
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Формируем JSON вручную
        String json = String.format("{\"status\": %d, \"message\": \"%s\"}", status.value(), message);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

