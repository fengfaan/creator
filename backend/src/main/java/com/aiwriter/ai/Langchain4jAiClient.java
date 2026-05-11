package com.aiwriter.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.V;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class Langchain4jAiClient implements AiClient {
    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_ANTHROPIC_BASE_URL = "https://token-plan-cn.xiaomimimo.com/anthropic";
    private final BaoyuSkillsSupport baoyuSkillsSupport;
    private final BaoyuImagineTool baoyuImagineTool;
    private final BaoyuGeminiWebTool baoyuGeminiWebTool;

    public Langchain4jAiClient() {
        this(BaoyuSkillsSupport.disabled(), null, null);
    }

    @Autowired
    public Langchain4jAiClient(
            BaoyuSkillsSupport baoyuSkillsSupport,
            BaoyuImagineTool baoyuImagineTool,
            BaoyuGeminiWebTool baoyuGeminiWebTool
    ) {
        this.baoyuSkillsSupport = baoyuSkillsSupport;
        this.baoyuImagineTool = baoyuImagineTool;
        this.baoyuGeminiWebTool = baoyuGeminiWebTool;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        try {
            ChatModel model = isAnthropicMessages(request.baseUrl())
                    ? anthropicModel(request)
                    : openAiModel(request);
            String text = baoyuSkillsSupport.available()
                    ? completeWithBaoyuSkills(model, request.messages())
                    : completeDirectly(model, request.messages());
            if (text == null || text.isBlank()) {
                throw new AiClientException(502, "AI 返回内容为空");
            }
            return new ChatResponse(text.trim(), request.model());
        } catch (AiClientException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new AiClientException(400, "Base URL 或模型配置不正确: " + e.getMessage());
        } catch (Exception e) {
            throw new AiClientException(502, "AI 服务调用失败: " + e.getMessage());
        }
    }

    private String completeDirectly(ChatModel model, List<ChatMessage> messages) {
        var langchainRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(toLangchainMessages(messages))
                .build();
        var response = model.chat(langchainRequest);
        return response.aiMessage() == null ? "" : response.aiMessage().text();
    }

    private String completeWithBaoyuSkills(ChatModel model, List<ChatMessage> messages) {
        SkillAwareAssistant assistant = AiServices.builder(SkillAwareAssistant.class)
                .chatModel(model)
                .tools(baoyuTools())
                .toolProvider(baoyuSkillsSupport.toolProvider())
                .systemMessage(baoyuSkillsSupport.systemMessage(systemMessages(messages)))
                .maxSequentialToolsInvocations(8)
                .build();
        return assistant.chat(conversationPrompt(messages));
    }

    private List<Object> baoyuTools() {
        List<Object> tools = new java.util.ArrayList<>();
        if (baoyuImagineTool != null) {
            tools.add(baoyuImagineTool);
        }
        if (baoyuGeminiWebTool != null) {
            tools.add(baoyuGeminiWebTool);
        }
        return tools;
    }

    String openAiBaseUrl(String baseUrl) {
        String value = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_OPENAI_BASE_URL : baseUrl.trim();
        String trimmed = value.replaceAll("/+$", "");
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed.substring(0, trimmed.length() - "/chat/completions".length());
        }
        return trimmed;
    }

    String anthropicBaseUrl(String baseUrl) {
        String value = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_ANTHROPIC_BASE_URL : baseUrl.trim();
        String trimmed = value.replaceAll("/+$", "");
        if (trimmed.endsWith("/v1/messages")) {
            return trimmed.substring(0, trimmed.length() - "/v1/messages".length());
        }
        if (trimmed.endsWith("/messages")) {
            return trimmed.substring(0, trimmed.length() - "/messages".length());
        }
        if (trimmed.endsWith("/v1") && trimmed.contains("xiaomimimo.com")) {
            return trimmed.substring(0, trimmed.length() - 3) + "/anthropic";
        }
        return trimmed;
    }

    boolean isAnthropicMessages(String baseUrl) {
        if (baseUrl == null) {
            return false;
        }
        String normalized = baseUrl.toLowerCase();
        return normalized.contains("/anthropic") || normalized.contains("xiaomimimo.com");
    }

    private ChatModel openAiModel(ChatRequest request) {
        return OpenAiChatModel.builder()
                .baseUrl(openAiBaseUrl(request.baseUrl()))
                .apiKey(request.apiKey())
                .modelName(request.model())
                .temperature(request.temperature())
                .maxTokens(request.maxTokens())
                .timeout(Duration.ofSeconds(180))
                .build();
    }

    private ChatModel anthropicModel(ChatRequest request) {
        return AnthropicChatModel.builder()
                .baseUrl(anthropicBaseUrl(request.baseUrl()))
                .apiKey(request.apiKey())
                .modelName(request.model())
                .temperature(request.temperature())
                .maxTokens(request.maxTokens())
                .timeout(Duration.ofSeconds(180))
                .build();
    }

    private List<dev.langchain4j.data.message.ChatMessage> toLangchainMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(this::toLangchainMessage)
                .toList();
    }

    private dev.langchain4j.data.message.ChatMessage toLangchainMessage(ChatMessage message) {
        String content = message.content() == null ? "" : message.content();
        return switch (message.role()) {
            case "system" -> SystemMessage.from(content);
            case "assistant" -> AiMessage.from(content);
            default -> UserMessage.from(content);
        };
    }

    String systemMessages(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> "system".equals(message.role()))
                .map(ChatMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    String conversationPrompt(List<ChatMessage> messages) {
        String prompt = messages.stream()
                .filter(message -> !"system".equals(message.role()))
                .map(message -> "%s:\n%s".formatted(message.role(), message.content() == null ? "" : message.content()))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
        return prompt.isBlank() ? "" : prompt;
    }

    interface SkillAwareAssistant {
        @dev.langchain4j.service.UserMessage("{{message}}")
        String chat(@V("message") String message);
    }
}
