package com.aiwriter.service;

import com.aiwriter.ai.BaoyuImagineTool;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.AiImageResponse;
import com.aiwriter.model.ConfigItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AiImageService {
    private static final String DEFAULT_ZAI_IMAGE_MODEL = "glm-image";

    private final ConfigService configService;
    private final AiImagePromptFactory promptFactory;
    private final BaoyuImagineTool baoyuImagineTool;
    private final ObjectMapper objectMapper;

    public AiImageService(
            ConfigService configService,
            AiImagePromptFactory promptFactory,
            BaoyuImagineTool baoyuImagineTool,
            ObjectMapper objectMapper
    ) {
        this.configService = configService;
        this.promptFactory = promptFactory;
        this.baoyuImagineTool = baoyuImagineTool;
        this.objectMapper = objectMapper;
    }

    public AiImageResponse generate(AiImageRequest request) {
        AiImageBrief brief = promptFactory.buildBrief(request);
        return generateWithBaoyuImagine(request, brief);
    }

    private AiImageResponse generateWithBaoyuImagine(AiImageRequest request, AiImageBrief brief) {
        String provider = "zai";
        String model = imageModel();
        if (firstConfig("zai_api_key", "bigmodel_api_key", "image_api_key").isBlank()) {
            throw new AiWritingException(400, "请先在设置中配置 GLM/Z.AI 图片 API Key");
        }
        String result = baoyuImagineTool.generateImage(
                brief.prompt(),
                aspectRatio(request),
                imageQuality(),
                provider,
                model,
                promptFactory.normalizePurpose(request)
        );
        try {
            JsonNode root = objectMapper.readTree(result);
            if (!root.path("success").asBoolean(false)) {
                String message = root.path("error").asText("GLM 图片生成失败");
                throw new AiWritingException(502, message);
            }
            String assetPath = root.path("assetPath").asText("");
            String publicUrl = root.path("publicUrl").asText("");
            String absolutePath = root.path("absolutePath").asText("");
            String resolvedModel = root.path("model").asText(model);
            String markdown = "![" + brief.alt() + "](" + assetPath + ")";
            if (!brief.caption().isBlank()) {
                markdown += "\n\n> " + brief.caption();
            }
            return new AiImageResponse(
                    markdown,
                    assetPath,
                    publicUrl,
                    absolutePath,
                    brief.prompt(),
                    brief.alt(),
                    brief.caption(),
                    provider + "/" + resolvedModel
            );
        } catch (AiWritingException e) {
            throw e;
        } catch (Exception e) {
            throw new AiWritingException(502, "GLM 图片生成结果解析失败: " + e.getMessage());
        }
    }

    private String imageModel() {
        String model = configValue("image_model", DEFAULT_ZAI_IMAGE_MODEL).trim();
        return model.isBlank() ? DEFAULT_ZAI_IMAGE_MODEL : model;
    }

    private String imageQuality() {
        String quality = configValue("image_quality", "2k").trim().toLowerCase(Locale.ROOT);
        return quality.equals("normal") ? "normal" : "2k";
    }

    private String aspectRatio(AiImageRequest request) {
        return switch (promptFactory.normalizePurpose(request)) {
            case "hero" -> "16:9";
            case "cover" -> "3:4";
            default -> "1:1";
        };
    }

    private String firstConfig(String... keys) {
        for (String key : keys) {
            String value = configValue(key, "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String configValue(String key, String fallback) {
        ConfigItem item = configService.get(key);
        if (item == null || item.getValue() == null || item.getValue().isBlank()) {
            return fallback;
        }
        return item.getValue();
    }
}
