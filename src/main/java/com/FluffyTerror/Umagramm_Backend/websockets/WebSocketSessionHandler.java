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
                case "KEY_EXCHANGE_REQUEST","KEY_EXCHANGE_RESPONSE":
                {
                    String targetId = json.get("targetId").asText();
                    String myPublicKey = json.get("publicKey").asText();
                    String currentType = json.get("type").asText();

                    WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);

                    if (targetSession != null && targetSession.isOpen()) {
                        Map<String, String> response = Map.of(
                                "type", currentType,
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
                case "GROUP_KEY_DISTRIBUTION": {
                    String chatId = json.get("chatId").asText();
                    String encryptedKey = json.get("encryptedKey").asText();
                    log.info("Incoming GROUP_KEY_DISTRIBUTION msg {}",json);
                    // Проверяем наличие версии ключа (полезно для re-keying при кике)
                    long keyVersion = json.has("keyVersion") ? json.get("keyVersion").asLong() : 1L;

                    // 1. Получаем список ID друзей в этом чате
                    return friendsService.getFriendsByChatId(chatId, userId)
                            // Если запрос возвращает Flux<String>, map не нужен. Если Flux<Friend>, то:
                            // .map(Friend::getFriendId)
                            .collectList()
                            .flatMap(allowedFriendIds -> {
                                List<Mono<Void>> sendTasks = new ArrayList<>();

                                // 2. Рассылаем один и тот же зашифрованный блок всем друзьям онлайн
                                for (String targetId : allowedFriendIds) {
                                    WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);

                                    if (targetSession != null && targetSession.isOpen()) {
                                        Map<String, Object> response = Map.of(
                                                "type", "GROUP_KEY_RECEIVED",
                                                "chatId", chatId,
                                                "fromId", userId,
                                                "encryptedKey", encryptedKey,
                                                "keyVersion", keyVersion
                                        );
                                        sendTasks.add(sendJson(targetSession, response));
                                    }
                                }

                                // 3. Выполняем рассылку параллельно
                                return Mono.when(sendTasks);
                            });
                }
                case "IS_KEY_REQUIRED": {
                    String chatId = json.get("chatId").asText();
                    log.info("Incoming IS_KEY_REQUIRED msg {}",json);
                    // userId здесь — это ID того, КТО спрашивает (отправитель сообщения)

                    // 1. Находим всех друзей в этом чате
                    return friendsService.getFriendsByChatId(chatId, userId)
                            .collectList()
                            .flatMap(allowedFriendIds -> {
                                List<Mono<Void>> sendTasks = new ArrayList<>();

                                for (String targetId : allowedFriendIds) {
                                    WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);

                                    // 2. Проверяем, в сети ли друг
                                    if (targetSession != null && targetSession.isOpen()) {
                                        Map<String, Object> response = Map.of(
                                                "type", "IS_KEY_REQUIRED",
                                                "chatId", chatId,
                                                "senderId", userId // Пересылаем ID того, кто готов дать ключ
                                        );
                                        sendTasks.add(sendJson(targetSession, response));
                                    }
                                }
                                // 3. Рассылаем уведомление всем друзьям в чате
                                return Mono.when(sendTasks);
                            });
                }case "KEY_NEEDED_REPLY": {
                    String chatId = json.get("chatId").asText();
                    String targetId = json.get("targetId").asText(); // ID того, кто прислал IS_KEY_REQUIRED (Админ)
                    log.info("Incoming KEY_NEEDED_REPLY msg {}",json);
                    WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);

                    if (targetSession != null && targetSession.isOpen()) {
                        Map<String, Object> response = Map.of(
                                "type", "KEY_NEEDED_REPLY",
                                "chatId", chatId,
                                "fromId", userId // ID того, кому нужен ключ (Кент)
                        );
                        return sendJson(targetSession, response);
                    }
                    return Mono.empty();
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