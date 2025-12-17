package com.prepxl.streaming.handler;

import com.prepxl.streaming.service.GeminiService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TranscriptionHandler implements WebSocketHandler {

    private final GeminiService geminiService;

    public TranscriptionHandler(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<byte[]> audioInput = session.receive()
                .filter(msg -> msg.getType() == WebSocketMessage.Type.BINARY)
                .map(msg -> {
                    byte[] bytes = new byte[msg.getPayload().readableByteCount()];
                    msg.getPayload().read(bytes);
                    return bytes;
                });

        Flux<WebSocketMessage> textOutput = geminiService.transcribe(audioInput)
                .map(session::textMessage);

        return session.send(textOutput);
    }
}
