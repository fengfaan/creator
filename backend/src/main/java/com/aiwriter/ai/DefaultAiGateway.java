package com.aiwriter.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultAiGateway implements AiGateway {
    private final AiClient langchain4jAiClient;
    private final AiClient xiaomiTokenPlanAiClient;
    private final AiImageClient aiImageClient;

    @Autowired
    public DefaultAiGateway(
            Langchain4jAiClient langchain4jAiClient,
            XiaomiTokenPlanAiClient xiaomiTokenPlanAiClient,
            DefaultAiImageClient aiImageClient
    ) {
        this((AiClient) langchain4jAiClient, xiaomiTokenPlanAiClient, aiImageClient);
    }

    DefaultAiGateway(AiClient langchain4jAiClient, AiClient xiaomiTokenPlanAiClient, AiImageClient aiImageClient) {
        this.langchain4jAiClient = langchain4jAiClient;
        this.xiaomiTokenPlanAiClient = xiaomiTokenPlanAiClient;
        this.aiImageClient = aiImageClient;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        if (isXiaomiTokenPlan(request.baseUrl())) {
            return xiaomiTokenPlanAiClient.complete(request);
        }
        return langchain4jAiClient.complete(request);
    }

    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        return aiImageClient.generate(request);
    }

    boolean isXiaomiTokenPlan(String baseUrl) {
        return baseUrl != null && baseUrl.toLowerCase().contains("xiaomimimo.com");
    }
}
