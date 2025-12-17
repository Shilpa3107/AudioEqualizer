package com.prepxl.streaming.service;

import reactor.core.publisher.Flux;

public interface GeminiService {
    Flux<String> transcribe(Flux<byte[]> audioStream);
}
