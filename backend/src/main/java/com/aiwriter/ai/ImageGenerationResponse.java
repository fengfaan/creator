package com.aiwriter.ai;

public record ImageGenerationResponse(byte[] bytes, String model, String format) {
}
