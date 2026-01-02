package com.FluffyTerror.Umagramm_Backend.service.notification;

import com.FluffyTerror.Umagramm_Backend.models.notification.Notification;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> sendNotification(String userId, Notification notification) {

        WebSocketSession session = registry.get(userId);
        if (session == null) {
            log.warn("User {} is offline", userId);
            return Mono.empty();
        }

        try {
            String payload = objectMapper.writeValueAsString(notification);
            WebSocketMessage message = session.textMessage(payload);

            return session.send(Mono.just(message));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
