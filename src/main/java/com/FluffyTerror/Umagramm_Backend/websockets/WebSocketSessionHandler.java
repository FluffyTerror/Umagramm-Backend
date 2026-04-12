package com.FluffyTerror.Umagramm_Backend.websockets;

import com.FluffyTerror.Umagramm_Backend.controller.user.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.service.friends.FriendsService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageService;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class WebSocketSessionHandler implements WebSocketHandler {
    private final JwtService jwtService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final MessageService messageService;
    private final FriendsService friendsService;
    private final UserService userService;
    private final ObjectMapper objectMapper;


    public String getToken(String query) {
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
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

            userService.updateUserOnlineStatus(userId)
                    .then(userService.broadcastStatus(userId, true))
                    .subscribe();

            // Поток ВХОДЯЩИХ сообщений от клиента (SEEN)
            Mono<Void> input = session.receive()
                    .flatMap(message -> {
                        String payload = message.getPayloadAsText();
                        return handleClientAction(payload, userId, session);
                    })
                    .then();

            // Поток уведомлений пользователю (в NotificationService)
            // doOnTerminate почистит реестр
            return input.doFinally(signal -> {
                webSocketSessionRegistry.unregister(userId);

                // ✅ OFFLINE
                userService.updateUserOnlineStatus(userId)
                        .then(userService.broadcastStatus(userId, false))
                        .subscribe();
            });

        } catch (Exception e) {
            return session.close(CloseStatus.NORMAL);
        }
    }

    private Mono<Void> handleClientAction(String payload, String userId, WebSocketSession session) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";

            switch (type) {

                case "SEEN": {
                    String chatId = json.get("chatId").asText();
                    Long lastId = json.get("lastMessageId").asLong();
                    return messageService.setMessagesToSeen(chatId, lastId, userId);
                }

                case "FRIEND_REQUEST": {
                    String receiverId = json.get("receiverId").asText();

                    return friendsService.createFriendsRequest(userId, receiverId)
                            .flatMap(request -> {
                                WebSocketSession receiverSession = webSocketSessionRegistry.get(receiverId);

                                if (receiverSession != null && receiverSession.isOpen()) {
                                    String notification = "{\"type\":\"FRIEND_REQUEST\"}";
                                    return receiverSession.send(
                                            Mono.just(receiverSession.textMessage(notification))
                                    );
                                }
                                return Mono.empty();
                            });
                }

                case "STATUS": {
                    List<String> ids = new ArrayList<>();

                    if (json.has("ids") && json.get("ids").isArray()) {
                        json.get("ids").forEach(node -> ids.add(node.asText()));
                    }

                    if (ids.isEmpty()) {
                        return session.send(Mono.just(
                                session.textMessage("{\"type\":\"STATUS_LIST\",\"users\":[]}")
                        ));
                    }

                    UsersRequest usersRequest = new UsersRequest(ids);

                    return userService.getInfoAboutUsers(userId, usersRequest)
                            .flatMap(userResponses -> {
                                try {
                                    String responseJson = objectMapper.writeValueAsString(Map.of(
                                            "type", "STATUS_LIST",
                                            "users", userResponses
                                    ));
                                    return session.send(
                                            Mono.just(session.textMessage(responseJson))
                                    );
                                } catch (JsonProcessingException e) {
                                    return Mono.error(e);
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("STATUS error", e);
                                return session.send(Mono.just(
                                        session.textMessage("{\"type\":\"ERROR\",\"message\":\"STATUS failed\"}")
                                ));
                            });
                }

                default:
                    log.warn("Unknown WS type: {}", type);
                    return Mono.empty();
            }

        } catch (Exception e) {
            log.error("WS parse error", e);
            return Mono.empty();
        }
    }
}