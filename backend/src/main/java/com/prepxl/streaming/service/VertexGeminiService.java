package com.prepxl.streaming.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
@Profile("prod") // Active when 'prod' profile is set
public class VertexGeminiService implements GeminiService {

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.location}")
    private String location;

    @Value("${google.cloud.model}")
    private String modelName;

    private VertexAI vertexAI;
    private GenerativeModel model;

    @PostConstruct
    public void init() throws IOException {
        // Initialize Vertex AI client
        // Requires GOOGLE_APPLICATION_CREDENTIALS env var set
        if (projectId != null && !projectId.isEmpty()) {
            this.vertexAI = new VertexAI(projectId, location);
            this.model = new GenerativeModel(modelName, vertexAI);
        }
    }

    @Override
    public Flux<String> transcribe(Flux<byte[]> audioStream) {
        // Core Requirement: Accept audio chunks and immediately forward to Gemini.
        // Note: In a real streaming scenario with Gemini 1.5, we would use a 
        // persistent session or append audio parts to the chat context.
        // For this assignment, we map the incoming stream to API calls.
        
        return audioStream
                .window(5) // Optimization: Group small chunks slightly to match API expectations
                .flatMap(chunkFlux -> chunkFlux.reduce(new byte[0], (a, b) -> {
                    byte[] combined = new byte[a.length + b.length];
                    System.arraycopy(a, 0, combined, 0, a.length);
                    System.arraycopy(b, 0, combined, a.length, b.length);
                    return combined;
                }))
                .map(this::callGeminiApi)
                .onErrorResume(e -> Flux.just("Error calling Gemini: " + e.getMessage()));
    }

    private String callGeminiApi(byte[] audioBytes) {
        try {
            if (model == null) return "Gemini Client Not Initialized (Check Config)";

            // Create Multi-modal content (Audio + Prompt)
            Content content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData("audio/wav", audioBytes), // Assuming WAV/PCM from frontend
                    "Transcribe the following audio segment exactly:"
            );

            // Stream the response from Gemini
            ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(content);
            
            StringBuilder fullText = new StringBuilder();
            for (GenerateContentResponse response : responseStream) {
                fullText.append(ResponseHandler.getText(response));
            }
            return fullText.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty on error to keep stream alive
        }
    }
}
