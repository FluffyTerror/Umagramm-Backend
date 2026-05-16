package com.FluffyTerror.Umagramm_Backend.websockets;

import com.FluffyTerror.Umagramm_Backend.requests.groupChat.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.service.friends.FriendsService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.message.MessageService;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return extractToken(session)
                .flatMap(token -> {
                    String userId = jwtService.extractUserId(token).toString();
                    webSocketSessionRegistry.register(userId, session);

                    // Установка статуса -> Входной поток -> Выход
                    return userService.updateUserOnlineStatus(userId)
                            .then(userService.broadcastStatus(userId, true))
                            .thenMany(session.receive())
                            .flatMap(message -> handleClientAction(message.getPayloadAsText(), userId, session))
                            .doFinally(signal -> handleDisconnect(userId))
                            .then();
                })
                .onErrorResume(e -> {
                    log.error("WS Session Error: {}", e.getMessage());
                    return session.close(CloseStatus.BAD_DATA);
                });
    }

    private Mono<String> extractToken(WebSocketSession session) {
        List<String> headers = session.getHandshakeInfo().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.isEmpty() || !headers.getFirst().startsWith("Bearer ")) {
            return Mono.error(new RuntimeException("Missing or invalid Authorization header"));
        }
        return Mono.just(headers.getFirst().substring(7));
    }

    private void handleDisconnect(String userId) {
        webSocketSessionRegistry.unregister(userId);
        userService.updateUserOnlineStatus(userId)
                .then(userService.broadcastStatus(userId, false))
                .subscribe();
    }

    private Mono<Void> handleClientAction(String payload, String userId, WebSocketSession session) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "UNKNOWN";

            return switch (type) {
                case "SEEN" -> handleSeenAction(json, userId);
                case "FRIEND_REQUEST" -> handleFriendRequest(json, userId, session);
                case "STATUS" -> handleStatusRequest(json, userId, session);
                case "KEY_EXCHANGE_REQUEST", "KEY_EXCHANGE_RESPONSE" -> handleKeyExchange(json, userId, type);
                case "ACCEPT_FRIEND_REQUEST" -> handleAcceptFriend(json, userId);
                case "GROUP_KEY_DISTRIBUTION" -> handleGroupKeyDistribution(json, userId);
                case "IS_KEY_REQUIRED" -> handleKeyRequired(json, userId);
                case "KEY_NEEDED_REPLY" -> handleKeyNeededReply(json, userId);
                case "CHAT_MEMBER_ADDED", "CHAT_MEMBER_REMOVED" -> handleChatMemberUpdate(json, userId, type);
                case "OLD_KEYS_FOR_OLD_HAGS"->handleOldHagsKeyDistribution(json,userId);
                default -> {
                    log.warn("Unknown WS type: {} from user: {}", type, userId);
                    yield Mono.empty();
                }
            };
        } catch (Exception e) {
            log.error("Error parsing WS payload", e);
            return Mono.empty();
        }
    }


    private Mono<Void> handleSeenAction(JsonNode json, String userId) {
        String chatId = json.get("chatId").asText();
        Long lastId = json.get("lastMessageId").asLong();
        return messageService.setMessagesToSeen(chatId, lastId, userId);
    }

    private Mono<Void> handleFriendRequest(JsonNode json, String userId, WebSocketSession session) {
        String receiverId = json.get("receiverId").asText();
        return friendsService.createFriendsRequest(userId, receiverId)
                .flatMap(request -> {
                    Mono<Void> notify = sendToUser(receiverId, Map.of("type", "FRIEND_REQUEST"));
                    Mono<Void> sendStatus = userService.getInfoAboutUsers(userId, new UsersRequest(List.of(receiverId)))
                            .flatMap(res -> sendJson(session, Map.of("type", "STATUS_LIST", "users", res)));
                    return Mono.when(notify, sendStatus);
                });
    }

    private Mono<Void> handleStatusRequest(JsonNode json, String userId, WebSocketSession session) {
        List<String> ids = new ArrayList<>();
        if (json.has("ids") && json.get("ids").isArray()) {
            json.get("ids").forEach(node -> ids.add(node.asText()));
        }

        if (ids.isEmpty()) return sendJson(session, Map.of("type", "STATUS_LIST", "users", List.of()));

        return userService.getInfoAboutUsers(userId, new UsersRequest(ids))
                .flatMap(res -> sendJson(session, Map.of("type", "STATUS_LIST", "users", res)))
                .onErrorResume(e -> sendJson(session, Map.of("type", "ERROR", "message", "STATUS failed")));
    }

    private Mono<Void> handleKeyExchange(JsonNode json, String userId, String type) {
        String targetId = json.get("targetId").asText();
        return sendToUser(targetId, Map.of(
                "type", type,
                "fromId", userId,
                "publicKey", json.get("publicKey").asText()
        ));
    }

    private Mono<Void> handleAcceptFriend(JsonNode json, String userId) {
        String requesterId = json.get("requesterId").asText();
        return friendsService.acceptFriendRequest(userId, requesterId)
                .flatMap(friendship -> sendToUser(requesterId, Map.of(
                        "type", "FRIEND_ACCEPTED_HANDSHAKE",
                        "fromId", userId,
                        "publicKey", json.get("publicKey").asText()
                )));
    }

    private Mono<Void> handleGroupKeyDistribution(JsonNode json, String userId) {
        String chatId = json.get("chatId").asText();
        return broadcastToChatFriends(chatId, userId, Map.of(
                "type", "GROUP_KEY_RECEIVED",
                "chatId", chatId,
                "fromId", userId,
                "encryptedKey", json.get("encryptedKey").asText(),
                "keyVersion", json.has("keyVersion") ? json.get("keyVersion").asLong() : 1L
        ));
    }

    private Mono<Void> handleKeyRequired(JsonNode json, String userId) {
        String chatId = json.get("chatId").asText();

        if (json.has("keyVersion")) {
            return broadcastToChatFriends(chatId, userId, Map.of(
                    "type", "IS_KEY_REQUIRED",
                    "chatId", chatId,
                    "senderId", userId,
                    "keyVersion", json.get("keyVersion").asLong()
            ));
        } else {
            return broadcastToChatFriends(chatId, userId, Map.of(
                    "type", "IS_KEY_REQUIRED",
                    "chatId", chatId,
                    "senderId", userId
            ));
        }
    }
    private Mono<Void> handleOldHagsKeyDistribution(JsonNode json, String userId) {
        String chatId = json.get("chatId").asText();
        return broadcastToChatFriends(chatId, userId, Map.of(
                "type", "OLD_KEYS_FOR_OLD_HAGS",
                "chatId", chatId,
                "senderId", userId,
                "encryptedKey", json.get("encryptedKey").asText(),
                "keyVersion", json.has("keyVersion") ? json.get("keyVersion").asLong() : 1L
        ));
    }

    private Mono<Void> handleKeyNeededReply(JsonNode json, String userId) {
        String targetId = json.get("targetId").asText();
        return sendToUser(targetId, Map.of(
                "type", "KEY_NEEDED_REPLY",
                "chatId", json.get("chatId").asText(),
                "fromId", userId
        ));
    }

    private Mono<Void> handleChatMemberUpdate(JsonNode json, String userId, String type) {
        String chatId = json.get("chatId").asText();
        String targetId = json.get("targetId").asText();
        Map<String, Object> payload = Map.of(
                "type", type,
                "chatId", chatId,
                "targetId", targetId,
                "actorId", userId
        );

        Mono<Void> notifyTarget = sendToUser(targetId, payload);
        Mono<Void> notifyOthers = broadcastToChatFriends(chatId, userId, payload, List.of(targetId));

        return Mono.when(notifyTarget, notifyOthers);
    }


    private Mono<Void> sendToUser(String targetId, Object data) {
        WebSocketSession targetSession = webSocketSessionRegistry.get(targetId);
        if (targetSession != null && targetSession.isOpen()) {
            return sendJson(targetSession, data);
        }
        return Mono.empty();
    }

    private Mono<Void> broadcastToChatFriends(String chatId, String userId, Object data) {
        return broadcastToChatFriends(chatId, userId, data, List.of());
    }

    private Mono<Void> broadcastToChatFriends(String chatId, String userId, Object data, List<String> excludeIds) {
        return friendsService.getFriendsByChatId(chatId, userId)
                .filter(id -> !id.equals(userId) && !excludeIds.contains(id))
                .flatMap(targetId -> sendToUser(targetId, data))
                .then();
    }

    private Mono<Void> sendJson(WebSocketSession session, Object data) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(data))
                .flatMap(json -> session.send(Mono.just(session.textMessage(json))))
                .doOnError(e -> log.error("JSON Error", e));
    }
}