package com.FluffyTerror.Umagramm_Backend.websockets;

import com.FluffyTerror.Umagramm_Backend.requests.UsersRequest;
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

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        List<String> headers = session.getHandshakeInfo().getHeaders().get("Authorization");

        if (headers == null || headers.isEmpty()) {
            return session.close(CloseStatus.BAD_DATA);
        }
        String authHeader = headers.getFirst(); // Берем первый элемент списка
        String token = authHeader.replace("Bearer ", "");
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

            // Поток уведомлений пользователю
            return input.doFinally(signal -> {
                webSocketSessionRegistry.unregister(userId);
                // OFFLINE
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
                                // 1. Уведомляем получателя (если он в сети)
                                WebSocketSession receiverSession = webSocketSessionRegistry.get(receiverId);
                                Mono<Void> notifyReceiver = Mono.empty();

                                if (receiverSession != null && receiverSession.isOpen()) {
                                    String notification = "{\"type\":\"FRIEND_REQUEST\"}";
                                    notifyReceiver = receiverSession.send(
                                            Mono.just(receiverSession.textMessage(notification))
                                    );
                                }

                                // 2. СРАЗУ запрашиваем статус получателя для отправителя
                                UsersRequest statusRequest = new UsersRequest(List.of(receiverId));
                                Mono<Void> sendStatusToSender = userService.getInfoAboutUsers(userId, statusRequest)
                                        .flatMap(userResponses -> {
                                            Map<String, Object> response = Map.of(
                                                    "type", "STATUS_LIST",
                                                    "users", userResponses
                                            );
                                            return sendJson(session, response); // Отправляем СЕБЕ (отправителю)
                                        });

                                // Выполняем оба действия параллельно
                                return Mono.when(notifyReceiver, sendStatusToSender);
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
                case "KEY_EXCHANGE": {
                    String targetId = json.get("targetId").asText();
                    String myPublicKey = json.get("publicKey").asText();

                    WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);

                    if (targetSession != null && targetSession.isOpen()) {
                        Map<String, String> response = Map.of(
                                "type", "KEY_EXCHANGE_COMPLETE",
                                "fromId", userId,
                                "publicKey", myPublicKey
                        );
                        return sendJson(targetSession, response);
                    }
                    return Mono.empty();
                }
                case "ACCEPT_FRIEND_REQUEST": {
                    String requesterId = json.get("requesterId").asText();
                    String myPublicKey = json.get("publicKey").asText();
                    log.info("incoming ACCEPT: userId={}, requesterId={}", userId, requesterId);

                    return friendsService.acceptFriendRequest(userId, requesterId)
                            .flatMap(friendship -> {
                                log.info("DB updated. looking for session for requesterId={}", requesterId);
                                WebSocketSession requesterSession = webSocketSessionRegistry.get(requesterId);

                                if (requesterSession != null && requesterSession.isOpen()) {
                                    log.info("session found. sending FRIEND_ACCEPTED_HANDSHAKE...");
                                    Map<String, String> response = Map.of(
                                            "type", "FRIEND_ACCEPTED_HANDSHAKE",
                                            "fromId", userId,
                                            "publicKey", myPublicKey
                                    );
                                    return sendJson(requesterSession, response);
                                } else {
                                    log.warn("Session not found. requesterId={} currently OFFLINE.", requesterId);
                                    return Mono.empty();
                                }
                            })
                            // Если сервис ничего не вернул (Mono.empty)
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("friendsService returned void! request not found or already accepted");
                                return Mono.empty();
                            }))
                            // Перехват ошибок базы данных или бизнес-логики
                            .onErrorResume(e -> {
                                log.error("Error inside friendsService: ", e);
                                return Mono.empty();
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
    private Mono<Void> sendJson(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (JsonProcessingException e) {
            log.error("JSON serializing error", e);
            return Mono.error(e);
        }
    }
}