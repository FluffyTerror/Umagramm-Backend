package com.FluffyTerror.Umagramm_Backend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр активных WebSocket-сессий.
 *
 * <p>
 * Полный аналог:
 * <pre>
 * convertAndSendToUser(userId, ...)
 * </pre>
 */
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void unregister(String userId) {
        sessions.remove(userId);
    }

    public WebSocketSession get(String userId) {
        return sessions.get(userId);
    }
}
