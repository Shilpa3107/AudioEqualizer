package com.prepxl.audio.handler;

import com.prepxl.audio.service.TranscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;

@Component
@Slf4j
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final TranscriptionService transcriptionService;

    public AudioWebSocketHandler(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] audioData = message.getPayload().array();
        
        // Forward to Gemini service and handle the streamed response
        transcriptionService.transcribeStream(audioData)
                .subscribe(partialText -> {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(partialText));
                        }
                    } catch (IOException e) {
                        log.error("Error sending partial transcription", e);
                    }
                });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {}", session.getId());
    }
}
