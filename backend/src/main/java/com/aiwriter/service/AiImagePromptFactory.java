package com.aiwriter.service;

import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.ChatMessage;
import com.aiwriter.ai.ChatRequest;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.ConfigItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AiImagePromptFactory {
    private final ConfigService configService;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    public AiImagePromptFactory(ConfigService configService, AiGateway aiGateway, ObjectMapper objectMapper) {
        this.configService = configService;
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
    }

    AiImageBrief buildBrief(AiImageRequest request) {
        String textApiKey = configValue("ai_api_key", "").trim();
        if (textApiKey.isBlank()) {
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
                            new ChatMessage("system", """
                                    你是中文内容产品的视觉总监。请根据文章上下文，为图片生成模型产出严格 JSON。
                                    只输出 JSON，不要 Markdown，不要解释。图片中不要出现可读文字、logo、水印、二维码。
                                    JSON 字段：prompt、alt、caption。
                                    """),
                            new ChatMessage("user", briefUserPrompt(request))
                    ),
                    0.45,
                    2048
            )).text();
            return parseBrief(text, request);
        } catch (RuntimeException e) {
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
        return """
                图片用途：%s
                画幅建议：%s

                标题：
                %s

                正文：
                %s

                光标附近/参考内容：
                %s

                请产出适合当前内容的图片 brief。prompt 用英文或中英混合均可，但要明确主体、场景、氛围、风格、构图和避让项。
                """.formatted(
                purposeLabel(request),
                imageSize(request),
                blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章"),
                excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 4000),
                blankAs(safeText(request == null ? null : request.getReferenceText()), "无")
        );
    }

    private String xhsCoverBriefUserPrompt(AiImageRequest request) {
        return """
                图片用途：小红书封面，竖版 3:4，像信息卡/封面图，而不是普通插画。
                画幅建议：%s

                标题：
                %s

                正文：
                %s

                光标附近/参考内容：
                %s

                请产出严格 JSON：prompt、alt、caption。
                prompt 必须适合 Pollinations 直接生成图片，并内化这些小红书封面规则：
                - Portrait 3:4 cover, strong first-screen hook, sparse layout, one clear focal object.
                - Xiaohongshu style infographic cover, clean editorial composition, rounded information-card blocks, sticker-like accents, soft shadows.
                - Keep a safe content area, avoid important details in top-right and bottom 10%%.
                - Use 1-2 large empty title-card shapes or abstract headline blocks, but do NOT render readable text.
                - Prefer hand-drawn editorial illustration or polished flat illustration; no photorealistic screenshot, no UI screenshot.
                - Use a fresh, warm, high-engagement palette with enough contrast; avoid clutter.
                - No readable text, no Chinese characters, no logo, no watermark, no QR code.
                - Keep prompt under 700 English words; prefer compact visual keywords over long explanation.
                alt 用中文简短描述画面；caption 可为空或一句中文说明。
                """.formatted(
                imageSize(request),
                blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章"),
                excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 3000),
                blankAs(safeText(request == null ? null : request.getReferenceText()), "无")
        );
    }

    private AiImageBrief parseBrief(String text, AiImageRequest request) {
        try {
            String json = stripJsonFence(text);
            JsonNode root = objectMapper.readTree(json);
            String prompt = root.path("prompt").asText("").trim();
            String alt = root.path("alt").asText("").trim();
            String caption = root.path("caption").asText("").trim();
            if (prompt.isBlank()) {
                return fallbackBrief(request);
            }
            return new AiImageBrief(
                    addPromptGuardrails(prompt),
                    blankAs(alt, "根据文章内容生成的配图"),
                    caption
            );
        } catch (Exception e) {
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
        String prompt = """
                Create a polished editorial image for a Chinese article.
                Purpose: %s.
                Article title: %s.
                Context: %s.
                Style: modern editorial illustration, realistic details, thoughtful composition, natural lighting, high quality.
                Avoid readable text, logos, watermarks, QR codes, UI screenshots, clutter, distorted hands, low resolution.
                """.formatted(purposeLabel(request), title, content);
        return new AiImageBrief(addPromptGuardrails(prompt), "根据文章《" + title + "》生成的配图", "");
    }

    private AiImageBrief fallbackXhsCoverBrief(AiImageRequest request) {
        String title = blankAs(safeText(request == null ? null : request.getTitle()), "未命名文章");
        String content = excerpt(blankAs(safeText(request == null ? null : request.getContent()), "暂无正文"), 900);
        String prompt = """
                Xiaohongshu Little Red Book style cover image, portrait 3:4, clean infographic cover.
                Topic: %s. Context: %s.
                Sparse layout, one clear focal object, rounded information-card blocks, abstract headline-card shapes,
                sticker-like accents, soft shadows, fresh warm palette, polished flat editorial illustration,
                safe content area, no important details in top-right or bottom 10 percent.
                No readable text, no Chinese characters, no logos, no watermarks, no QR codes, no UI screenshot.
                """.formatted(title, content);
        return new AiImageBrief(addPromptGuardrails(prompt), "根据文章《" + title + "》生成的小红书封面", "");
    }

    private String addPromptGuardrails(String prompt) {
        return prompt + "\nNo readable text, no logos, no watermarks, no QR codes.";
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
