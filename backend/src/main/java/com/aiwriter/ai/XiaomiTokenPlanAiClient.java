package com.aiwriter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class XiaomiTokenPlanAiClient implements AiClient {
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public XiaomiTokenPlanAiClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    XiaomiTokenPlanAiClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        ObjectNode payload = buildPayload(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(messagesUrl(request.baseUrl())))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("x-api-key", request.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiClientException(502, upstreamErrorMessage(response.body(), response.statusCode()));
            }
            return new ChatResponse(extractText(response.body()), request.model());
        } catch (AiClientException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new AiClientException(400, "Base URL 格式不正确");
        } catch (IOException e) {
            throw new AiClientException(502, "AI 服务连接失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException(502, "AI 请求已中断");
        }
    }

    String messagesUrl(String baseUrl) {
        String value = (baseUrl == null || baseUrl.isBlank())
                ? "https://token-plan-cn.xiaomimimo.com/anthropic"
                : baseUrl.trim();
        String trimmed = value.replaceAll("/+$", "");
        if (trimmed.endsWith("/v1/messages")) {
            return trimmed;
        }
        if (trimmed.endsWith("/messages")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1") && trimmed.contains("xiaomimimo.com")) {
            return trimmed.substring(0, trimmed.length() - 3) + "/anthropic/v1/messages";
        }
        if (trimmed.endsWith("/anthropic")) {
            return trimmed + "/v1/messages";
        }
        return trimmed + "/v1/messages";
    }

    private ObjectNode buildPayload(ChatRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", request.model());
        payload.put("max_tokens", request.maxTokens());
        payload.put("temperature", request.temperature());

        String system = request.messages().stream()
                .filter(message -> "system".equals(message.role()))
                .map(ChatMessage::content)
                .collect(Collectors.joining("\n\n"));
        if (!system.isBlank()) {
            payload.put("system", system);
        }

        ArrayNode messages = payload.putArray("messages");
        for (ChatMessage message : request.messages()) {
            if ("system".equals(message.role())) {
                continue;
            }
            messages.add(message(message.role(), message.content()));
        }
        return payload;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private String extractText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("content");
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText())) {
                    text.append(item.path("text").asText(""));
                }
            }
            if (!text.isEmpty()) {
                return text.toString().trim();
            }
        }
        throw new AiClientException(502, "AI 返回内容为空");
    }

    private String upstreamErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (message.isBlank()) {
                message = root.path("message").asText("");
            }
            if (!message.isBlank()) {
                return "AI 服务返回错误 (" + statusCode + "): " + message;
            }
        } catch (Exception ignored) {
        }
        return "AI 服务返回错误 (" + statusCode + ")";
    }
}
