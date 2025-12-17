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
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
