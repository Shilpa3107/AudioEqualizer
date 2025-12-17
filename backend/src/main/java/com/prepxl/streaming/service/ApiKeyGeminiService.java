package com.prepxl.streaming.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "gemini.api.key")
public class ApiKeyGeminiService implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ApiKeyGeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<String> transcribe(Flux<byte[]> audioStream) {
        // With the REST API (non-streaming input), we must buffer chunks to form a valid request.
        // We buffer ~2 seconds of audio (assuming approx byte rate) to send usable chunks.
        // Note: This is "pseudostreaming" - sending sequential requests.
        // True Bi-directional streaming requires gRPC or WebSocket API which is complex in Java.
        
        return audioStream
                .window(10) // Group 10 chunks (adjust based on chunk size from frontend)
                .flatMap(window -> window.reduce(new byte[0], (a, b) -> {
                    byte[] combined = new byte[a.length + b.length];
                    System.arraycopy(a, 0, combined, 0, a.length);
                    System.arraycopy(b, 0, combined, a.length, b.length);
                    return combined;
                }))
                .flatMapSequential(this::callGeminiRestApi);
    }

    private Flux<String> callGeminiRestApi(byte[] audioBytes) {
        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // Construct Gemini Protocol JSON
            // { "contents": [{ "parts": [{ "text": "Transcribe..." }, { "inlineData": { "mimeType": "audio/wav", "data": "..." } }] }] }
            Map<String, Object> inlineData = Map.of(
                    "mimeType", "audio/wav", // Ensure frontend sends this or we wrap raw PCM if supported
                    "data", base64Audio
            );
            
            Map<String, Object> audioPart = Map.of("inlineData", inlineData);
            Map<String, Object> textPart = Map.of("text", "Please transcribe this audio segment to English text. Output ONLY the transcription.");
            
            Map<String, Object> content = Map.of("parts", List.of(textPart, audioPart));
            Map<String, Object> requestBody = Map.of("contents", List.of(content));

            return webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(this::extractTextFromJson)
                    .flux()
                    .onErrorResume(e -> {
                        // System.err.println("Gemini API Error: " + e.getMessage());
                        return Flux.empty();
                    });

        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private String extractTextFromJson(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
           return "";
        }
        return "";
    }
}
