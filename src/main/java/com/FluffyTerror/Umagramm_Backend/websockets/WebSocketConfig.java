package com.FluffyTerror.Umagramm_Backend.websockets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandler(WebSocketSessionHandler handler){
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/messages", handler);
        int order = 1; // before annotated controllers
        return new SimpleUrlHandlerMapping(map, order);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {//тк я использую свою конфигурацию WebFlux, делаем свой бин
        return new WebSocketHandlerAdapter();
    }
}



