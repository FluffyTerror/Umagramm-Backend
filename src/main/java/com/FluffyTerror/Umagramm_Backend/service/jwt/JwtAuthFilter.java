package com.FluffyTerror.Umagramm_Backend.service.jwt;

import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

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

        try {
            UUID userId = jwtService.extractUserId(token);
            System.out.println(">>> Token extracted for User ID: " + userId); // ЛОГ 1

            return userRepository.findById(userId.toString())
                    .doOnNext(user -> System.out.println(">>> User found in DB: " + user.getUsername())) // ЛОГ 2
                    .map(user -> new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()))
                    .flatMap(auth -> chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                    )
                    .switchIfEmpty(Mono.defer(() -> {
                        System.out.println(">>> USER NOT FOUND IN DB!"); // ЛОГ 3
                        return chain.filter(exchange);
                    }))
                    .onErrorResume(e -> {
                        System.out.println(">>> JWT Error: " + e.getMessage()); // ЛОГ 4
                        return chain.filter(exchange);
                    });
        } catch (Exception e) {
            return chain.filter(exchange);
        }
    }
}

