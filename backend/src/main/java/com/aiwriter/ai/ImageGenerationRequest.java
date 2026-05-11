package com.aiwriter.ai;

public record ImageGenerationRequest(
        String apiKey,
        String baseUrl,
        String model,
        String prompt,
        String size
) {
}
