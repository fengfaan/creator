package com.aiwriter.service;

import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.ImageGenerationRequest;
import com.aiwriter.ai.ImageGenerationResponse;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.AiImageResponse;
import com.aiwriter.model.ConfigItem;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AiImageService {
    private static final String DEFAULT_IMAGE_BASE_URL = "https://image.pollinations.ai";
    private static final String DEFAULT_IMAGE_MODEL = "sana";

    private final ConfigService configService;
    private final AiImagePromptFactory promptFactory;
    private final AiGateway aiGateway;
    private final ImageAssetService imageAssetService;

    public AiImageService(
            ConfigService configService,
            AiImagePromptFactory promptFactory,
            AiGateway aiGateway,
            ImageAssetService imageAssetService
    ) {
        this.configService = configService;
        this.promptFactory = promptFactory;
        this.aiGateway = aiGateway;
        this.imageAssetService = imageAssetService;
    }

    public AiImageResponse generate(AiImageRequest request) {
        AiImageBrief brief = promptFactory.buildBrief(request);
        GeneratedImage image = toGeneratedImage(aiGateway.generateImage(imageGenerationRequest(brief, request)));
        return saveGeneratedImage(request, brief, image);
    }

    private ImageGenerationRequest imageGenerationRequest(AiImageBrief brief, AiImageRequest request) {
        String baseUrl = configValue("image_base_url", DEFAULT_IMAGE_BASE_URL).trim();
        String model = configValue("image_model", DEFAULT_IMAGE_MODEL).trim();
        String resolvedBaseUrl = baseUrl.isBlank() ? DEFAULT_IMAGE_BASE_URL : baseUrl;
        String resolvedModel = model.isBlank() ? DEFAULT_IMAGE_MODEL : model;
        String apiKey = resolveImageApiKey(resolvedBaseUrl);
        if (!usesPollinations(resolvedBaseUrl) && apiKey.isBlank()) {
            throw new AiWritingException(400, "请先在设置中配置图片 API Key，或把图片 Base URL 设为 image.pollinations.ai 使用免 Key 图片生成");
        }
        return new ImageGenerationRequest(
                apiKey,
                resolvedBaseUrl,
                resolvedModel,
                brief.prompt(),
                promptFactory.imageSize(request)
        );
    }

    private GeneratedImage toGeneratedImage(ImageGenerationResponse response) {
        ImageFormat format = "jpg".equalsIgnoreCase(response.format()) || "jpeg".equalsIgnoreCase(response.format())
                ? ImageFormat.JPEG
                : ImageFormat.PNG;
        return new GeneratedImage(response.bytes(), response.model(), format);
    }

    private AiImageResponse saveGeneratedImage(AiImageRequest request, AiImageBrief brief, GeneratedImage image) {
        try {
            ImageAssetService.SavedImage saved = switch (image.format()) {
                case JPEG -> imageAssetService.saveJpeg(image.bytes(), promptFactory.normalizePurpose(request));
                case PNG -> imageAssetService.savePng(image.bytes(), promptFactory.normalizePurpose(request));
            };
            String markdown = "![" + brief.alt() + "](" + saved.assetPath() + ")";
            if (!brief.caption().isBlank()) {
                markdown += "\n\n> " + brief.caption();
            }
            return new AiImageResponse(
                    markdown,
                    saved.assetPath(),
                    saved.publicUrl(),
                    saved.absolutePath(),
                    brief.prompt(),
                    brief.alt(),
                    brief.caption(),
                    image.model()
            );
        } catch (IOException e) {
            throw new AiWritingException(500, "图片保存失败: " + e.getMessage());
        }
    }

    private boolean usesPollinations(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim().toLowerCase();
        return value.isBlank()
                || value.equals("pollinations")
                || value.contains("image.pollinations.ai");
    }

    private String resolveImageApiKey(String imageBaseUrl) {
        String imageApiKey = configValue("image_api_key", "").trim();
        if (!imageApiKey.isBlank()) {
            return imageApiKey;
        }
        String aiBaseUrl = configValue("ai_base_url", AiWritingService.DEFAULT_BASE_URL).trim();
        if (sameService(imageBaseUrl, aiBaseUrl)) {
            return configValue("ai_api_key", "").trim();
        }
        return "";
    }

    private boolean sameService(String left, String right) {
        try {
            java.net.URI leftUri = new java.net.URI(left == null || left.isBlank() ? DEFAULT_IMAGE_BASE_URL : left);
            java.net.URI rightUri = new java.net.URI(right == null || right.isBlank() ? AiWritingService.DEFAULT_BASE_URL : right);
            String leftHost = leftUri.getHost();
            String rightHost = rightUri.getHost();
            return leftHost != null && rightHost != null && leftHost.equalsIgnoreCase(rightHost);
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    private String configValue(String key, String fallback) {
        ConfigItem item = configService.get(key);
        if (item == null || item.getValue() == null || item.getValue().isBlank()) {
            return fallback;
        }
        return item.getValue();
    }
}
