package com.FluffyTerror.Umagramm_Backend.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация RAW WebSocket
 *
 * <p>
 * Является функциональным аналогом STOMP-конфигурации:
 * <ul>
 *     <li>Нет брокера</li>
 *     <li>Нет @MessageMapping</li>
 *     <li>Полный контроль над routing'ом</li>
 * </ul>
 *
 * <p>
 * Сервер выступает ИСКЛЮЧИТЕЛЬНО как транспорт:
 * <ul>
 *     <li>не расшифровывает сообщения</li>
 *     <li>не анализирует payload</li>
 *     <li>не хранит ключи</li>
 * </ul>
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig {
    @Autowired
    private final WebSocketMessageHandler webSocketMessageHandler;

    /**
     * Регистрирует WebSocket endpoint.
     *
     * <p>
     * Аналог STOMP:
     * <pre>
     * registry.addEndpoint("/ws")
     * </pre>
     *
     * <p>
     * Клиент (Android / Kotlin) подключается по адресу:
     * <pre>
     * ws://host:port/ws
     * </pre>
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws", webSocketMessageHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    /**
     * Адаптер WebSocketHandler → WebFlux runtime.
     *
     * <p>
     * Без этого WebSocket не будет работать.
     */
    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
