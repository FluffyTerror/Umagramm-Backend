package com.FluffyTerror.Umagramm_Backend.service.friends;

import com.FluffyTerror.Umagramm_Backend.exceptions.TokenForQRCodeExpiredException;
import com.FluffyTerror.Umagramm_Backend.responses.qr.QRResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRService {
    public static final String PREFIX = "invite:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    /*
    Метод для генерации токена на основе которого будет сгенерирован QR Code
    */
    public Mono<QRResponse> generateToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = PREFIX + token;
        QRResponse response = new QRResponse(key);
        return reactiveRedisTemplate.opsForValue().set(key, userId, TTL).thenReturn(response);
    }

    /*
    Метод для получения id юзера после сканирования QR Code
    */
    public Mono<String> consumeToken(String token) {
        String key = PREFIX + token;
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(userId ->
                        reactiveRedisTemplate.delete(key).thenReturn(userId))
                .switchIfEmpty(Mono.error(new TokenForQRCodeExpiredException("Token is not valid or expired")));
    }
}

