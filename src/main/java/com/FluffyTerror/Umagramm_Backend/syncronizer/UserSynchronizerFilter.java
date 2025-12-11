package com.FluffyTerror.Umagramm_Backend.syncronizer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Реактивный фильтр, который выполняет синхронизацию пользователя с Keycloak
 * при каждом аутентифицированном запросе.
 *
 * <p><b>Порядок работы (по шагам):</b></p>
 * <ol>
 *   <li>Запрос проходит через систему безопасности Spring WebFlux.</li>
 *   <li>JWT-токен валидируется, и Spring помещает Authentication в
 *       {@link org.springframework.security.core.context.ReactiveSecurityContextHolder}.</li>
 *   <li>Этот фильтр вызывается после прохождения security-обработки.</li>
 *   <li>Фильтр получает Authentication из реактивного контекста.</li>
 *   <li>Если это {@link JwtAuthenticationToken} — вызывает синхронизатор пользователя.</li>
 *   <li>После завершения синхронизации — передаёт обработку дальше по цепочке.</li>
 * </ol>
 *
 * <p><b>Почему WebFilter, а не OncePerRequestFilter?</b><br>
 * Потому что WebFlux использует неблокирующий стек Reactor,
 * где сервлетные фильтры не работают. WebFilter — реактивный аналог.</p>
 *
 * <p><b>Важно:</b><br>
 * Метод <code>chain.filter(exchange)</code> ОБЯЗАТЕЛЬНО должен вызываться после
 * синхронизации, иначе выполнение запроса остановится.</p>
 */

@Component
@RequiredArgsConstructor
public class UserSynchronizerFilter implements WebFilter {
    private final UserSynchronizer userSynchronizer;

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext().flatMap(context -> {
            if (context.getAuthentication() instanceof JwtAuthenticationToken jwtToken) {
                return userSynchronizer.synchronizeWithIdp(jwtToken.getToken());
            }
            return chain.filter(exchange);
        }).switchIfEmpty(chain.filter(exchange));
    }
}
