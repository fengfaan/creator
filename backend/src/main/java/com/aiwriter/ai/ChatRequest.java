package com.aiwriter.ai;

import java.util.List;

public record ChatRequest(
        String apiKey,
        String baseUrl,
        String model,
        List<ChatMessage> messages,
        double temperature,
        int maxTokens
) {
}
