package com.aiwriter.ai;

public interface AiGateway {
    ChatResponse complete(ChatRequest request);

    ImageGenerationResponse generateImage(ImageGenerationRequest request);
}
