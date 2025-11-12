package com.FluffyTerror.Umagramm_Backend.security;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code KeyCloakAuthenticationConverter} — конвертер, предназначенный для преобразования JWT-токенов,
 * выданных Keycloak, в объекты аутентификации Spring Security ({@link JwtAuthenticationToken}).
 *
 * <p>Spring Security по умолчанию умеет работать только с "scope"-ролями, но Keycloak добавляет
 * информацию о ролях внутри claim-а {@code "resource_access"} в структуре, специфичной для клиентов (resources).
 * Данный конвертер объединяет стандартные роли и роли, выданные Keycloak, в единый набор {@link GrantedAuthority}.
 *
 * <p>Роли Keycloak преобразуются в формат, совместимый со Spring Security:
 * каждая роль получает префикс {@code ROLE_}, а дефисы заменяются на подчёркивания.
 * Например, {@code "manage-account"} → {@code "ROLE_MANAGE_ACCOUNT"}.
 *
 * <p>Используется как часть цепочки аутентификации Spring Security,
 * обычно регистрируется как bean в конфигурации Security через:
 * <pre>{@code
 * http.oauth2ResourceServer()
 *     .jwt()
 *     .jwtAuthenticationConverter(new KeyCloakAuthenticationConverter());
 * }</pre>
 *
 * @author FluffyTerror
 * @since 1.0
 */
public class KeyCloakAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    /**
     * Преобразует входящий {@link Jwt} токен в {@link JwtAuthenticationToken}, понятный Spring Security.
     *
     * <p>Метод объединяет роли, полученные:
     * <ul>
     *     <li>из стандартного claim-а {@code "scope"} (через {@link JwtGrantedAuthoritiesConverter})</li>
     *     <li>из Keycloak claim-а {@code "resource_access"} (через {@link #extractResourceRoles(Jwt)})</li>
     * </ul>
     *
     * <p>В результате формируется единый набор {@link GrantedAuthority},
     * используемый Spring Security для проверки прав доступа.
     *
     * @param source объект {@link Jwt}, содержащий данные токена от Keycloak
     * @return объект {@link JwtAuthenticationToken}, содержащий JWT и список ролей пользователя
     */
    @Override
    public Mono<AbstractAuthenticationToken> convert(@NonNull Jwt source) {
        //создаем JWT Токен с некими правами которые мы спарсили из Keycloak
        var authorities = new JwtGrantedAuthoritiesConverter().convert(source);
        var keycloakAuthorities = extractResourceRoles(source);

        var allAuthorities = Stream.concat(authorities.stream(), keycloakAuthorities.stream()).collect(Collectors.toSet());
        return Mono.just(new JwtAuthenticationToken(source, allAuthorities));
    }

    /**
     * Извлекает роли из раздела {@code "resource_access"} токена Keycloak и преобразует их
     * в формат {@link GrantedAuthority}, понятный Spring Security.
     *
     * <p>Пример структуры claim-а Keycloak:
     * <pre>{@code
     * "resource_access": {
     *   "account": {
     *     "roles": ["manage-account", "view-profile"]
     *   },
     *   "my-backend": {
     *     "roles": ["user", "moderator"]
     *   }
     * }
     * }</pre>
     *
     * <p>Метод:
     * <ol>
     *     <li>Извлекает claim {@code resource_access}</li>
     *     <li>Проваливается в  {@code "account"}</li>
     *     <li>Преобразует список ролей в объекты {@link SimpleGrantedAuthority},
     *     добавляя префикс {@code ROLE_} и заменяя дефисы на подчёркивания</li>
     * </ol>
     *
     * @param jwt объект {@link Jwt}, полученный от Keycloak
     * @return коллекция {@link GrantedAuthority}, представляющая роли пользователя
     */
    private Collection<? extends GrantedAuthority> extractResourceRoles(@NonNull Jwt jwt) {
        var resourceAccess = new HashMap<>(jwt.getClaim("resource_access"));

        var eternal = (Map<String, List<String>>) resourceAccess.get("account");

        var roles = eternal.get("roles");

        //тут происходит магия маппинга так как в keycloak все роли возвращаются в таком виде //role-name
        //И нам нужно их переделать чтобы Spring смог их разобрать
        return roles.stream().map(
                role -> new SimpleGrantedAuthority("ROLE_" + role.replace("-", "_"))
        ).collect(Collectors.toSet());
    }
}
