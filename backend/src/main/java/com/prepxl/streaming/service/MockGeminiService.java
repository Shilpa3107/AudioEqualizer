package com.prepxl.streaming.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(GeminiService.class)
public class MockGeminiService implements GeminiService {

    private final List<String> PHRASES = List.of(
        "The circular equalizer is responding to frequency...",
        "Streaming audio data in real-time...",
        "Analyzing spectral components...",
        "Connecting to Vertex AI...",
        "Transcription service active.",
        "Microphone input detected.",
        "Generating live captions...",
        "This is a simulated transcription response.",
        "Testing the UI latency...",
        "Java Spring Boot backend is handling the stream."
    );
    private final Random random = new Random();

    @Override
    public Flux<String> transcribe(Flux<byte[]> audioStream) {
        // In a real implementation, this would stream the bytes to Vertex AI.
        // Here we simulate transcription by returning realistic looking phrases.
        
        return audioStream
                .bufferTimeout(50, Duration.ofMillis(1500)) // Update text every ~1.5s
                .map(chunks -> {
                    return PHRASES.get(random.nextInt(PHRASES.size()));
                });
    }
}
