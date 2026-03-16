package com.FluffyTerror.Umagramm_Backend.websockets;

import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
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

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().toString();
        String token = getToken(query);
        if (token.isEmpty()) {
            session.close(CloseStatus.BAD_DATA);
        }
        try {
            String userId = jwtService.extractUserId(token).toString();
            webSocketSessionRegistry.register(userId,session);
            log.info("user with id {} connected to WebSocket session {}",userId,session.getId());
            return session.receive().doOnTerminate(()-> {
                log.info("user {} disconnected from WebSocket session {}",userId,session.getId());
            }).then();
        } catch (Exception e) {
            return session.close(CloseStatus.NORMAL);
        }

    }

    public String getToken(String query) {
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];//--TODO не уверен насчет второго сплита
        }
        return null;
    }
}
