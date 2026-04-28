package com.FluffyTerror.Umagramm_Backend.service.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class PushNotificationService {

    public void sendPushNotification(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();

        try {
            // FirebaseMessaging.send() блокирует поток.
            // Если его прерывают извне (InterruptedException), мы должны это поймать.
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent push: {}", response);
        } catch (Exception e) {
            log.error("Firebase error for token {}: {}", targetToken, e.getMessage());
        }
    }
}
