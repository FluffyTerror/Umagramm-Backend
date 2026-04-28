package com.FluffyTerror.Umagramm_Backend.service.notification;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.enums.NotificationType;
import com.FluffyTerror.Umagramm_Backend.models.notification.Notification;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.service.push.PushNotificationService;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;
    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> sendNotificationWS(String userId, Notification notification) {

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
    public Mono<Void> sendNotification(String userId, Notification notification) {
        // 1. Пытаемся отправить в сокет (независимо от результата идем дальше)
        Mono<Void> wsPush = sendNotificationWS(userId, notification)
                .onErrorResume(e -> {
                    log.error("WS error for user {}: {}", userId, e.getMessage());
                    return Mono.empty();
                });

        // 2. Логика Firebase
        Mono<Void> firebasePush = userRepository.findById(userId)
                .flatMap(user -> {
                    // ПРОВЕРКА: есть ли токен И включены ли уведомления на уровне БД
                    if (user.getFcmToken() != null && user.isNotificationsEnabled() && shouldSendPush(notification)) {
                        String title = notification.getChatName() != null ? notification.getChatName() : "Umagramm";
                        String body = formatPushBody(notification);

                        return Mono.fromRunnable(() -> {
                            pushNotificationService.sendPushNotification(user.getFcmToken(), title, body);
                        }).subscribeOn(Schedulers.boundedElastic()).then();
                    }
                    return Mono.empty();
                });

        // Используем when вместо zip, чтобы они работали параллельно и не блокировали друг друга
        return Mono.when(wsPush, firebasePush);
    }

    private boolean shouldSendPush(Notification notification) {
        // Не шлем пуши на статус прочтения (SEEN) или другие служебные типы,
        // чтобы не спамить пользователя
        return notification.getType() == NotificationType.MESSAGE;
    }

    private String formatPushBody(Notification notification) {
        if (notification.getMessageType() == null) {
            return "Новое сообщение";
        }
        return switch (notification.getMessageType()) {
            case TEXT -> "Новое сообщение"; // Вместо зашифрованного текста
            case IMAGE -> "📷 Фотография";
            case AUDIO -> "🎤 Голосовое сообщение";
            case VIDEO -> "🎥 Видео";
            case FILE -> "📁 Файл";
            case DOCUMENT -> "📄 Документ";
            case MIXED -> "📦 Вложение";
            case SYSTEM -> notification.getContent(); // Системные обычно не шифруются
            default -> "Новое уведомление";
        };
    }
}
