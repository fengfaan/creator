package com.aiwriter.service;

import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.BaoyuImagineTool;
import com.aiwriter.ai.ChatRequest;
import com.aiwriter.ai.ChatResponse;
import com.aiwriter.ai.ImageGenerationRequest;
import com.aiwriter.ai.ImageGenerationResponse;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.AiImageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiImageServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("ai.db"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """);
        configService = new ConfigService(jdbc);
    }

    @Test
    void coverGenerationAlwaysUsesGlmImageThroughBaoyuImagine() {
        configService.set("image_api_key", "glm-key");
        configService.set("image_model", "glm-image");
        CapturingBaoyuImagineTool baoyuTool = new CapturingBaoyuImagineTool(configService);
        AiImageService service = serviceWithBaoyu(baoyuTool);

        AiImageResponse response = service.generate(new AiImageRequest(
                "cover",
                "AI 写作工作流",
                "分享一套从选题到发布的小红书内容流程。",
                ""
        ));

        assertThat(baoyuTool.provider).isEqualTo("zai");
        assertThat(baoyuTool.model).isEqualTo("glm-image");
        assertThat(baoyuTool.aspectRatio).isEqualTo("3:4");
        assertThat(baoyuTool.quality).isEqualTo("2k");
        assertThat(baoyuTool.prompt)
                .contains("Xiaohongshu Little Red Book style cover image")
                .contains("No readable text");
        assertThat(response.getMarkdown()).isEqualTo("![根据文章《AI 写作工作流》生成的小红书封面](assets/images/test.png)");
        assertThat(response.getUrl()).isEqualTo("/api/v1/assets/images/test.png");
        assertThat(response.getModel()).isEqualTo("zai/glm-image");
    }

    @Test
    void glmGenerationRequiresImageApiKey() {
        AiImageService service = serviceWithBaoyu(new CapturingBaoyuImagineTool(configService));

        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(AiWritingException.class)
                .hasMessage("请先在设置中配置 GLM/Z.AI 图片 API Key")
                .satisfies(e -> assertThat(((AiWritingException) e).getStatus()).isEqualTo(400));
    }

    private AiImageService serviceWithBaoyu(BaoyuImagineTool baoyuTool) {
        PromptLoader promptLoader = new PromptLoader(configService);
        return new AiImageService(
                configService,
                new AiImagePromptFactory(
                        configService,
                        (TextOnlyGateway) request -> {
                            throw new AssertionError("Text AI gateway should not be called without an API key");
                        },
                        new ObjectMapper(),
                        promptLoader
                ),
                baoyuTool,
                new ObjectMapper()
        );
    }

    private interface TextOnlyGateway extends AiGateway {
        @Override
        default ImageGenerationResponse generateImage(ImageGenerationRequest request) {
            throw new AssertionError("Image generation should not be called while building prompts");
        }
    }

    private class CapturingBaoyuImagineTool extends BaoyuImagineTool {
        String prompt;
        String aspectRatio;
        String quality;
        String provider;
        String model;

        CapturingBaoyuImagineTool(ConfigService configService) {
            super(
                    configService,
                    new ObjectMapper(),
                    tempDir.resolve("skills/baoyu-imagine/scripts/main.ts").toString(),
                    tempDir.resolve("articles").toString()
            );
        }

        @Override
        public String generateImage(
                String prompt,
                String aspectRatio,
                String quality,
                String provider,
                String model,
                String purpose
        ) {
            this.prompt = prompt;
            this.aspectRatio = aspectRatio;
            this.quality = quality;
            this.provider = provider;
            this.model = model;
            return """
                    {
                      "success": true,
                      "assetPath": "assets/images/test.png",
                      "publicUrl": "/api/v1/assets/images/test.png",
                      "absolutePath": "/tmp/test.png",
                      "provider": "zai",
                      "model": "glm-image"
                    }
                    """;
        }
    }
}
