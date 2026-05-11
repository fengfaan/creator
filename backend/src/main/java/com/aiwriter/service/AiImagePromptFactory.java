package com.aiwriter.service;

import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.ChatMessage;
import com.aiwriter.ai.ChatRequest;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.ConfigItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiImagePromptFactory {
    private static final Logger log = LoggerFactory.getLogger(AiImagePromptFactory.class);

    private final ConfigService configService;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;
    private final PromptLoader prompts;

    public AiImagePromptFactory(ConfigService configService, AiGateway aiGateway, ObjectMapper objectMapper, PromptLoader prompts) {
        this.configService = configService;
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
        this.prompts = prompts;
    }

    AiImageBrief buildBrief(AiImageRequest request) {
        String textApiKey = configValue("ai_api_key", "").trim();
        if (textApiKey.isBlank()) {
            log.warn("Image brief: ai_api_key not configured, using fallback prompt for purpose={}", normalizePurpose(request));
            return fallbackBrief(request);
        }
        String baseUrl = configValue("ai_base_url", AiWritingService.DEFAULT_BASE_URL).trim();
        String model = configValue("selected_model", AiWritingService.DEFAULT_MODEL).trim();
        String resolvedBaseUrl = baseUrl.isBlank() ? AiWritingService.DEFAULT_BASE_URL : baseUrl;
        String resolvedModel = normalizeTextModel(model, resolvedBaseUrl);
        try {
            String text = aiGateway.complete(new ChatRequest(
                    textApiKey,
                    resolvedBaseUrl,
                    resolvedModel,
                    List.of(
                            new ChatMessage("system", prompts.get("image/brief-system")),
                            new ChatMessage("user", briefUserPrompt(request))
                    ),
                    0.45,
                    2048
            )).text();
            AiImageBrief brief = parseBrief(text, request);
            log.info("Image brief generated successfully, prompt length={}", brief.prompt().length());
            return brief;
        } catch (RuntimeException e) {
            log.warn("Image brief: text AI call failed ({}), using fallback prompt", e.getMessage());
            return fallbackBrief(request);
        }
    }

    String imageSize(AiImageRequest request) {
        return switch (normalizePurpose(request)) {
            case "hero" -> "1536x1024";
            case "cover" -> "1024x1365";
            default -> "1024x1024";
        };
    }

    String normalizePurpose(AiImageRequest request) {
        String value = safeText(request == null ? null : request.getPurpose()).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "hero", "cover", "inline" -> value;
            default -> "inline";
        };
    }

    private String briefUserPrompt(AiImageRequest request) {
        if ("cover".equals(normalizePurpose(request))) {
            return xhsCoverBriefUserPrompt(request);
        }
        String template = prompts.get("image/brief-user");
        return PromptLoader.format(template, Map.of(
                "purpose", purposeLabel(request),
                "aspect", imageSize(request),
                "title", blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章"),
                "content", excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 4000),
                "referenceText", blankAs(safeText(request == null ? null : request.getReferenceText()), "无")
        ));
    }

    private String xhsCoverBriefUserPrompt(AiImageRequest request) {
        String template = prompts.get("image/xhs-cover-brief-user");
        return PromptLoader.format(template, Map.of(
                "aspect", imageSize(request),
                "title", blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章"),
                "content", excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 3000),
                "referenceText", blankAs(safeText(request == null ? null : request.getReferenceText()), "无")
        ));
    }

    private AiImageBrief parseBrief(String text, AiImageRequest request) {
        try {
            String json = stripJsonFence(text);
            JsonNode root = objectMapper.readTree(json);
            String prompt = root.path("prompt").asText("").trim();
            String alt = root.path("alt").asText("").trim();
            String caption = root.path("caption").asText("").trim();
            if (prompt.isBlank()) {
                log.warn("Image brief: text AI returned JSON without prompt field, using fallback. Raw response: {}", text.substring(0, Math.min(text.length(), 200)));
                return fallbackBrief(request);
            }
            return new AiImageBrief(
                    addPromptGuardrails(prompt),
                    blankAs(alt, "根据文章内容生成的配图"),
                    caption
            );
        } catch (Exception e) {
            log.warn("Image brief: failed to parse text AI response as JSON ({}), using fallback. Raw: {}", e.getMessage(), text.substring(0, Math.min(text.length(), 200)));
            return fallbackBrief(request);
        }
    }

    private String purposeLabel(AiImageRequest request) {
        return switch (normalizePurpose(request)) {
            case "hero" -> "文章首图，横版，适合公众号/博客顶部";
            case "cover" -> "小红书封面，竖版，主体明确，有停留感";
            default -> "正文配图，方图，服务当前段落";
        };
    }

    private AiImageBrief fallbackBrief(AiImageRequest request) {
        if ("cover".equals(normalizePurpose(request))) {
            return fallbackXhsCoverBrief(request);
        }
        String title = blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章");
        String content = excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 1200);
        String template = prompts.get("image/fallback-user");
        String prompt = PromptLoader.format(template, Map.of(
                "purpose", purposeLabel(request),
                "title", title,
                "content", content
        ));
        return new AiImageBrief(addPromptGuardrails(prompt), "根据文章《" + title + "》生成的配图", "");
    }

    private AiImageBrief fallbackXhsCoverBrief(AiImageRequest request) {
        String title = blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章");
        String content = excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 900);
        String template = prompts.get("image/fallback-xhs-cover-user");
        String prompt = PromptLoader.format(template, Map.of(
                "title", title,
                "content", content
        ));
        return new AiImageBrief(addPromptGuardrails(prompt), "根据文章《" + title + "》生成的小红书封面", "");
    }

    private String addPromptGuardrails(String prompt) {
        return prompt + "\n" + prompts.get("image/guardrails");
    }

    private String normalizeTextModel(String model, String baseUrl) {
        String value = model == null || model.isBlank() ? AiWritingService.DEFAULT_MODEL : model.trim();
        if (baseUrl != null && baseUrl.toLowerCase(Locale.ROOT).contains("xiaomimimo.com")) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "mimo-v2.5-pro" -> "mimo-v2.5-pro";
                case "mimo-v2.5" -> "mimo-v2.5";
                case "mimo-v2-flash" -> "mimo-v2-flash";
                default -> value.replace("MiMo", "mimo").replace("V", "v").toLowerCase(Locale.ROOT);
            };
        }
        return value;
    }

    private String stripJsonFence(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String excerpt(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max) + "...";
    }
}
