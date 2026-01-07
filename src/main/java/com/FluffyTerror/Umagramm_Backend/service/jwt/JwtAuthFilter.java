package com.FluffyTerror.Umagramm_Backend.service.jwt;

import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
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

        // 1. Если нет заголовка — просто пропускаем запрос дальше
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
                        return Mono.empty();
                    }
                })
                .flatMap(userRepository::findById)
                .map(user -> new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()))
                // 2. Если нашли пользователя — упаковываем в Optional, чтобы Mono не был пустым!
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(authOpt -> {
                    // 3. ЗДЕСЬ вызываем chain.filter только один раз!
                    return authOpt.map(usernamePasswordAuthenticationToken -> chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(usernamePasswordAuthenticationToken))).orElseGet(() -> chain.filter(exchange));
                })
                .onErrorResume(e -> {
                    log.error("JWT Filter critical error: {}", e.getMessage());
                    // В случае ошибки — просто пропускаем запрос как неавторизованный
                    // Но сначала проверяем, не был ли ответ уже закоммичен
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    return chain.filter(exchange);
                });
    }
}

