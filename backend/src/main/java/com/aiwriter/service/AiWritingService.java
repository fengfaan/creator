package com.aiwriter.service;

import com.aiwriter.ai.AiClientException;
import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.ChatMessage;
import com.aiwriter.ai.ChatRequest;
import com.aiwriter.ai.ChatResponse;
import com.aiwriter.model.AiGenerateRequest;
import com.aiwriter.model.AiGenerateResponse;
import com.aiwriter.model.ConfigItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiWritingService {
    static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    static final String DEFAULT_MODEL = "deepseek-chat";

    private final ConfigService configService;
    private final AiWritingPromptFactory promptFactory;
    private final AiGateway aiGateway;

    public AiWritingService(
            ConfigService configService,
            AiWritingPromptFactory promptFactory,
            AiGateway aiGateway
    ) {
        this.configService = configService;
        this.promptFactory = promptFactory;
        this.aiGateway = aiGateway;
    }

    public AiGenerateResponse generate(AiGenerateRequest request) {
        AiWritingAction action = AiWritingAction.from(request == null ? null : request.getAction());
        String apiKey = configValue("ai_api_key", "").trim();
        if (apiKey.isBlank()) {
            throw new AiWritingException(400, "请先在设置中配置 API Key");
        }

        String baseUrl = configValue("ai_base_url", DEFAULT_BASE_URL).trim();
        String model = configValue("selected_model", DEFAULT_MODEL).trim();
        String resolvedBaseUrl = baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl;
        String resolvedModel = normalizeModel(model, resolvedBaseUrl);
        AiWritingPrompt prompt = promptFactory.build(action, request);

        try {
            ChatResponse response = aiGateway.complete(new ChatRequest(
                    apiKey,
                    resolvedBaseUrl,
                    resolvedModel,
                    List.of(
                            new ChatMessage("system", prompt.system()),
                            new ChatMessage("user", prompt.user())
                    ),
                    prompt.temperature(),
                    prompt.maxTokens()
            ));
            return new AiGenerateResponse(response.text(), response.model());
        } catch (AiClientException e) {
            throw new AiWritingException(e.getStatus(), e.getMessage());
        }
    }

    private String normalizeModel(String model, String baseUrl) {
        String value = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        if (baseUrl != null && baseUrl.toLowerCase().contains("xiaomimimo.com")) {
            return switch (value.toLowerCase()) {
                case "mimo-v2.5-pro" -> "mimo-v2.5-pro";
                case "mimo-v2.5" -> "mimo-v2.5";
                case "mimo-v2-flash" -> "mimo-v2-flash";
                default -> value.replace("MiMo", "mimo").replace("V", "v").toLowerCase();
            };
        }
        return value;
    }

    private String configValue(String key, String fallback) {
        ConfigItem item = configService.get(key);
        if (item == null || item.getValue() == null || item.getValue().isBlank()) {
            return fallback;
        }
        return item.getValue();
    }
}
