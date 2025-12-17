package com.prepxl.streaming.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class MockGeminiService implements GeminiService {

    @Override
    public Flux<String> transcribe(Flux<byte[]> audioStream) {
        // In a real implementation, this would stream the bytes to Vertex AI.
        // Here we simulate transcription by acknowledging data receipt.
        
        return audioStream
                .bufferTimeout(50, Duration.ofMillis(1000)) // Buffer chunks
                .map(chunks -> {
                    int size = chunks.stream().mapToInt(b -> b.length).sum();
                    return "Analyzed " + size + " bytes of audio... (Gemini Simulation)";
                });
    }
}
