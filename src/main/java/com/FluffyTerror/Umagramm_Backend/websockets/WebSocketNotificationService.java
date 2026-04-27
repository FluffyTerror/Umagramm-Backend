package com.FluffyTerror.Umagramm_Backend.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class WebSocketNotificationService {
    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public WebSocketNotificationService(WebSocketSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> sendToUser(String userId, Object payload) {
        WebSocketSession session = registry.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                return session.send(Mono.just(session.textMessage(json)));
            } catch (JsonProcessingException e) {
                return Mono.error(e);
            }
        }
        return Mono.empty();
    }

    public Mono<Void> broadcastToChat(List<String> userIds, Object payload) {
        return Flux.fromIterable(userIds)
                .flatMap(id -> sendToUser(id, payload))
                .then();
    }
}