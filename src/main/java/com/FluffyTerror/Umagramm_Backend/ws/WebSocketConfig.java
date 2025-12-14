package com.FluffyTerror.Umagramm_Backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Конфигурация WebSocket + STOMP для защищённого мессенджера.
 *
 * <p>
 * Сервер в данной архитектуре не расшифровывает сообщения и не анализирует контент.
 * Он выступает исключительно как транспорт (message relay) для:
 * <ul>
 *     <li>доставки PGP-зашифрованных сообщений</li>
 *     <li>уведомлений (доставка, прочтение)</li>
 *     <li>событий чатов</li>
 * </ul>
 *
 * Клиенты (мобильные Kotlin-приложения) полностью отвечают за:
 * <ul>
 *     <li>шифрование</li>
 *     <li>дешифрование</li>
 *     <li>хранение ключей</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Конфигурация брокера сообщений.
     *
     * <p>
     * Здесь определяется:
     * <ul>
     *     <li>куда сервер может отправлять сообщения</li>
     *     <li>какие префиксы считаются клиентскими</li>
     * </ul>
     *
     * <p>
     * В текущей конфигурации:
     * <ul>
     *     <li><b>/app</b> — входящие сообщения от клиента (send)</li>
     *     <li><b>/users</b> — персональные очереди пользователей</li>
     *     <li><b>/user</b> — внутренний брокер для point-to-point доставки</li>
     * </ul>
     *
     * Пример:
     * <pre>
     * Клиент отправляет: /app/chat.send
     * Сервер отвечает:   /users/{userId}/queue/messages
     * </pre>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Простой in-memory брокер (достаточно для MVP)
        registry.enableSimpleBroker("/user");

        // Все сообщения от клиента должны начинаться с /app
        registry.setApplicationDestinationPrefixes("/app");

        // Префикс для персональных очередей пользователей
        registry.setUserDestinationPrefix("/users");
    }

    /**
     * Регистрация WebSocket endpoint'а.
     *
     * <p>
     * Это HTTP endpoint, через который клиент устанавливает WebSocket-соединение.
     *
     * <p>
     * В твоём случае:
     * <ul>
     *     <li>Мобильный клиент подключается к <b>/ws</b></li>
     *     <li>Аутентификация происходит ДО апгрейда соединения</li>
     * </ul>
     *
     * <p>
     * SockJS добавлен для:
     * <ul>
     *     <li>fallback'ов</li>
     *     <li>стабильности соединения</li>
     * </ul>
     *
     * ⚠️ В проде:
     * <ul>
     *     <li>НЕ используй setAllowedOrigins("*")</li>
     *     <li>ограничь домены</li>
     * </ul>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*") // TODO ограничить
                .withSockJS();
    }

    /**
     * Добавляет поддержку AuthenticationPrincipal в WebSocket-хендлерах.
     *
     * <p>
     * Позволяет в @MessageMapping методах писать:
     *
     * <pre>
     * public void handle(@AuthenticationPrincipal UserDetails user)
     * </pre>
     *
     * <p>
     * Это критично для:
     * <ul>
     *     <li>trusted users</li>
     *     <li>валидации прав отправки сообщений</li>
     *     <li>доставки строго адресных сообщений</li>
     * </ul>
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    /**
     * Конфигурация JSON-конвертера для WebSocket сообщений.
     *
     * <p>
     * Все сообщения между клиентом и сервером:
     * <ul>
     *     <li>строго JSON</li>
     *     <li>контент — зашифрован (Base64 / ASCII Armor)</li>
     * </ul>
     *
     * <p>
     * Сервер:
     * <ul>
     *     <li>не валидирует содержимое</li>
     *     <li>не пытается интерпретировать payload</li>
     * </ul>
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MediaType.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper());
        converter.setContentTypeResolver(resolver);

        messageConverters.add(converter);

        // false = НЕ заменять стандартные конвертеры полностью
        return false;
    }
}
