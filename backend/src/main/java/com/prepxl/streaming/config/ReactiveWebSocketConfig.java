package com.prepxl.streaming.config;

import com.prepxl.streaming.handler.TranscriptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import java.util.Map;

@Configuration
public class ReactiveWebSocketConfig {

    private final TranscriptionHandler transcriptionHandler;

    public ReactiveWebSocketConfig(TranscriptionHandler transcriptionHandler) {
        this.transcriptionHandler = transcriptionHandler;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of("/stream", transcriptionHandler));
        mapping.setOrder(1);
        
        // CORS Configuration
        org.springframework.web.cors.CorsConfiguration corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
        corsConfiguration.addAllowedOrigin("http://localhost:5173"); // Allow Frontend
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:5173");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        
        mapping.setCorsConfigurations(Map.of("/stream", corsConfiguration));
        
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(webSocketService());
    }

    @Bean
    public org.springframework.web.reactive.socket.server.WebSocketService webSocketService() {
        org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService webSocketService = 
            new org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService();
        return webSocketService;
    }
}
