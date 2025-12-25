package com.FluffyTerror.Umagramm_Backend.service.mapper;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.service.user.UserResponse;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Маппер, превращающий данные из JWT-токена Keycloak (claims)
 * в локальную сущность {@link User}.
 *
 * <p><b>Задачи маппера:</b></p>
 * <ul>
 *   <li>Преобразовать структуру Keycloak в структуру локального пользователя.</li>
 *   <li>Взять корректные поля по стандартным claim-ключам Keycloak.</li>
 *   <li>Обновить имя, фамилию, username, время последней активности.</li>
 * </ul>
 *
 * <p><b>Пример картины данных Keycloak:</b></p>
 * <ul>
 *   <li>sub — уникальный ID пользователя в realm</li>
 *   <li>preferred_username — логин пользователя</li>
 *   <li>given_name — имя</li>
 *   <li>family_name — фамилия</li>
 *   <li>nickname — альтернативное имя</li>
 * </ul>
 *
 * <p><b>Важно:</b><br>
 * Этот класс только создает объект User.
 * Он НЕ выполняет сохранение в базу — это делает сервис синхронизации.</p>
 */
@Service
public class UserMapper {
    public User fromTokenAttributes(Map<String, Object> claims) {
        User user = new User();
        //--TODO возможно насрано тут
        if (claims.containsKey("sub")) {//получаем из токена инфу о пользователе и преобразуем ее в нашу сущность User
            user.setId(claims.get("sub").toString());
        }

        user.setUsername(
                claims.getOrDefault("preferred_username",
                        claims.getOrDefault("username", "undefined")
                ).toString()
        );

        if (claims.containsKey("given_name")) {
            user.setFirstName(claims.get("given_name").toString());
        } else if (claims.containsKey("nickname")) {
            user.setFirstName(claims.get("nickname").toString());
        }

        if (claims.containsKey("family_name")) {
            user.setLastName(claims.get("family_name").toString());
        }

        user.setLastSeen(LocalDateTime.now());
        return user;
    }

    public UserResponse toUserResponse (User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .lastSeen(user.getLastSeen())
                .isOnline(user.isOnline())
                .build();
    }
}
