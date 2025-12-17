package com.FluffyTerror.Umagramm_Backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {
        /*
         * Когда говорим про корсы по умолчанию - это значит, что Spring будет искать все бины типа {@code CorsFilter}
         * и будет получать все конфигурации для корсов.
         * <p>
         * Если ничего ему не передать - он по умолчанию возьмет свою конфигурацию, а она в свою очередь будет блокировать все запросы извне.
         * <p>
         * Для этого создан бин {@Link corsFilter}
         */
        http.cors(Customizer.withDefaults()).csrf(ServerHttpSecurity.CsrfSpec::disable)
                //передаем список запросов которые хотим авторизовать
                .authorizeExchange(req -> req.pathMatchers("/auth/**", "/v2/api-docs", "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui/**", "/webjars/**", "/swagger-ui.html", "/wss/**").permitAll().anyExchange().authenticated()).oauth2ResourceServer(auth -> auth.jwt(token -> token.jwtAuthenticationConverter(new KeyCloakAuthenticationConverter())));
        return http.build();
    }


    @Bean
    public CorsWebFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
        config.setAllowedHeaders(Arrays.asList(HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
