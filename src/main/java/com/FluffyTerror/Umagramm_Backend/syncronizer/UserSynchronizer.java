package com.FluffyTerror.Umagramm_Backend.syncronizer;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


/**
 * Сервис, отвечающий за синхронизацию пользователя из IDP (Keycloak)
 * с локальной базой данных приложения.
 *
 * <p><b>Когда вызывается синхронизация?</b><br>
 * Синхронизация выполняется на каждом аутентифицированном HTTP-запросе.
 * Вызов происходит из {@link com.FluffyTerror.Umagramm_Backend.syncronizer.UserSynchronizerFilter}.
 *
 * <p><b>Зачем это нужно?</b></p>
 * <ul>
 *   <li>При первом входе — создать пользователя в локальной БД.</li>
 *   <li>На последующих запросах — обновлять имя, фамилию, аватар, lastSeen и другие поля.</li>
 *   <li>Следить, что данные в БД актуальны относительно Keycloak.</li>
 * </ul>
 *
 * <p><b>Как работает метод:</b></p>
 * <ol>
 *   <li>Извлекает username из JWT (claim из Keycloak).</li>
 *   <li>Сопоставляет JWT claims с нашей сущностью {@link User} через {@link UserMapper}.</li>
 *   <li>Реактивно ищет в БД пользователя по username.</li>
 *   <li>Если пользователь уже существует — сохраняет ID и обновляет данные.</li>
 *   <li>Если пользователя нет — создаёт новую запись.</li>
 *   <li>Возвращает {@code Mono<Void>} — фильтр продолжает обработку запроса.</li>
 * </ol>
 *
 * <p><b>Почему метод возвращает Mono<Void>?</b><br>
 * WebFlux-фильтр должен вернуть реактивный поток завершения, чтобы:
 * <ul>
 *   <li>дождаться завершения синхронизации,</li>
 *   <li>продолжить выполнение цепочки фильтров.</li>
 * </ul>
 * <p>
 * Сервис не должен возвращать данные — он выполняет побочный эффект (save).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSynchronizer {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Mono<Void> synchronizeWithIdp(Jwt token) {
        log.info("Synchronizing user with idp (Keycloak)");
        String username = (String) token.getClaims().get("username");
        if (username == null) {
            return Mono.empty();
        }
        log.info("Synchronizing user with username {}", username);
        User newUser = userMapper.fromTokenAttributes(token.getClaims());

        // ищем пользователя в БД
        return userRepository.findUserByUsername(username)
                .defaultIfEmpty(new User()) // если нет — создаём нового
                .flatMap(existingUser -> {
                    // переносим ID, если пользователь существовал
                    newUser.setId(existingUser.getId());
                    return userRepository.save(newUser);
                }).switchIfEmpty(userRepository.save(newUser)).then();
    }

    private Mono<String> getUserUsername(Jwt token) {
        return Mono.justOrEmpty(token.getClaims().get("username").toString());
    }
}
