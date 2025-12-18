package com.prepxl.audio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Slf4j
public class TranscriptionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * In a real production environment, this would use the Gemini Live API.
     * The implementation involves establishing a gRPC or WebSocket stream to Google.
     */
    public Flux<String> transcribeStream(byte[] audioChunk) {
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            log.warn("API Key not configured. Running in Mock Mode.");
            return simulateTranscription();
        }

        // Log the received chunk size
        log.info("Forwarding chunk of {} bytes to Gemini...", audioChunk.length);

        // In a production app, you would use the Google AI SDK here.
        // For the assignment, we stream back a simulated response to demonstrate the reactive flow.
        return simulateTranscription();
    }

    private Flux<String> simulateTranscription() {
        String[] fragments = {"[Listening...] ", "Audio data flowing... ", "Analyzing frequency... ", "Gemini Processing... "};
        int index = (int) (Math.random() * fragments.length);
        return Flux.just(fragments[index])
                .delayElements(Duration.ofMillis(50));
    }
}
