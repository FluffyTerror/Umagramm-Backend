package com.FluffyTerror.Umagramm_Backend.security;

import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http){
        return http
                // 1. Настройка CORS
                .cors(Customizer.withDefaults())
                // 2. Отключаем CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 3. Настройка доступов
                .authorizeExchange(auth -> auth
                        // Разрешаем регистрацию и логин
                        .pathMatchers("/api/v1/auth/register", "/api/v1/auth/login","/api/v1/auth/nigga").permitAll()
                        // Разрешаем Swagger и Refresh
                        .pathMatchers(
                                "/api/v1/auth/refresh",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers("/ws/**").permitAll()// Разрешаю хендшейк, токен проверяю внутри Handler
                        .pathMatchers("/api/v1/attachments/**").permitAll()
                        // Все остальное — только по токену
                        .anyExchange().authenticated()
                )
                // 4. ДОБАВЛЯЕМ ВАШ JWT ФИЛЬТР
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }



    @Bean
    public CorsWebFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        // Рекомендуется использовать allowedOriginPatterns для гибкости
        config.setAllowedOriginPatterns(Collections.singletonList("http://localhost:*"));
        config.setAllowedHeaders(Arrays.asList(
                HttpHeaders.ORIGIN,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.AUTHORIZATION
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
