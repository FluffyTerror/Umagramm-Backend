package com.FluffyTerror.Umagramm_Backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * Основной WebSocket handler.
 *
 * <p>
 * Аналог STOMP:
 * <ul>
 *     <li>@MessageMapping</li>
 *     <li>/app</li>
 *     <li>/user/{id}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> {

                    String userId = auth.getName();
                    sessionRegistry.register(userId, session);

                    return session.receive()
                            .map(msg -> msg.getPayloadAsText())
                            .flatMap(payload -> handleMessage(payload, auth))
                            .doFinally(signal -> sessionRegistry.unregister(userId))
                            .then();
                });
    }

    private Mono<Void> handleMessage(String payload, Authentication auth) {
        try {
            WsEnvelope envelope = objectMapper.readValue(payload, WsEnvelope.class);

            return switch (envelope.type()) {
                case MESSAGE -> handleChatMessage(envelope, auth);
                case SEEN -> handleSeen(envelope, auth);
                case NOTIFICATION -> handleNotification(envelope, auth);
            };

        } catch (Exception e) {
            log.error("WS parse error", e);
            return Mono.empty();
        }
    }

    private Mono<Void> handleChatMessage(WsEnvelope envelope, Authentication auth) {
        // /app/chat.send аналог
        return Mono.empty();
    }

    private Mono<Void> handleSeen(WsEnvelope envelope, Authentication auth) {
        // /app/chat.seen аналог
        return Mono.empty();
    }

    private Mono<Void> handleNotification(WsEnvelope envelope, Authentication auth) {
        return Mono.empty();
    }
}
