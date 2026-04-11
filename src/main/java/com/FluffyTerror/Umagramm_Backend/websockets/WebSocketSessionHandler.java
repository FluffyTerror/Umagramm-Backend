package com.FluffyTerror.Umagramm_Backend.websockets;

import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class WebSocketSessionHandler implements WebSocketHandler {
    private final JwtService jwtService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();



    public String getToken(String query) {
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];//--TODO не уверен насчет второго сплита
        }
        return null;
    }
    // WebSocketSessionHandler.java

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().toString();
        String token = getToken(query);
        if (token == null || token.isEmpty()) {
            return session.close(CloseStatus.BAD_DATA);
        }

        try {
            String userId = jwtService.extractUserId(token).toString();
            webSocketSessionRegistry.register(userId, session);

            // Поток ВХОДЯЩИХ сообщений от клиента (SEEN)
            Mono<Void> input = session.receive()
                    .flatMap(message -> {
                        String payload = message.getPayloadAsText();
                        return handleClientAction(payload, userId);
                    })
                    .then();

            // Поток уведомлений пользователю (в NotificationService)
            // doOnTerminate почистит реестр
            return input.doFinally(signal -> webSocketSessionRegistry.unregister(userId));

        } catch (Exception e) {
            return session.close(CloseStatus.NORMAL);
        }
    }

    private Mono<Void> handleClientAction(String payload, String userId) {
        try {
            // Парсим JSON (нужен ObjectMapper)
            JsonNode json = objectMapper.readTree(payload);
            String type = json.get("type").asText();

            if ("SEEN".equals(type)) {
                String chatId = json.get("chatId").asText();
                Long lastId = json.get("lastMessageId").asLong();
                // Вызываем логику прочтения
                return messageService.setMessagesToSeen(chatId, lastId, userId);
            }
        } catch (Exception e) {
            log.error("WS parse error", e);
        }
        return Mono.empty();
    }
    private Mono<Void> handleIncomingWsMessage(String payload, String userId) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.get("type").asText();

            if ("SEEN".equals(type)) {
                String chatId = node.get("chatId").asText();
                Long lastMessageId = node.get("lastMessageId").asLong();
                // Вызываем твой метод из MessageService (лучше создать перегрузку без Authentication)
                return messageService.setMessagesToSeen(chatId, lastMessageId, userId);
            }
        } catch (Exception e) {
            log.error("WS processing error", e);
        }
        return Mono.empty();
    }
}
