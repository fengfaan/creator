package com.aiwriter.service;

import com.aiwriter.ai.AiGateway;
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
import java.util.concurrent.atomic.AtomicReference;

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
    void coverPromptUsesXhsCoverRulesAndThreeToFourSizeThroughGateway() {
        AtomicReference<ImageGenerationRequest> imageRequest = new AtomicReference<>();
        AiImageService service = serviceWithGateway(new CapturingGateway(imageRequest));

        AiImageResponse response = service.generate(new AiImageRequest(
                "cover",
                "AI 写作工作流",
                "分享一套从选题到发布的小红书内容流程。",
                ""
        ));

        assertThat(response.getPrompt())
                .contains("Xiaohongshu Little Red Book style cover image")
                .contains("portrait 3:4")
                .contains("rounded information-card blocks")
                .contains("No readable text");
        assertThat(imageRequest.get().baseUrl()).isEqualTo("https://image.pollinations.ai");
        assertThat(imageRequest.get().model()).isEqualTo("sana");
        assertThat(imageRequest.get().size()).isEqualTo("1024x1365");
    }

    @Test
    void openAiCompatibleProviderRequiresImageApiKeyBeforeCallingGateway() {
        configService.set("image_base_url", "https://api.openai.com/v1");
        configService.set("image_model", "gpt-image-1");
        AiImageService service = serviceWithGateway(new CapturingGateway(new AtomicReference<>()));

        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(AiWritingException.class)
                .hasMessage("请先在设置中配置图片 API Key，或把图片 Base URL 设为 image.pollinations.ai 使用免 Key 图片生成")
                .satisfies(e -> assertThat(((AiWritingException) e).getStatus()).isEqualTo(400));
    }

    private AiImageService serviceWithGateway(AiGateway gateway) {
        return new AiImageService(
                configService,
                new AiImagePromptFactory(
                        configService,
                        (TextOnlyGateway) request -> {
                            throw new AssertionError("Text AI gateway should not be called without an API key");
                        },
                        new ObjectMapper()
                ),
                gateway,
                new StubImageAssetService()
        );
    }

    private static class CapturingGateway implements AiGateway {
        private final AtomicReference<ImageGenerationRequest> imageRequest;

        CapturingGateway(AtomicReference<ImageGenerationRequest> imageRequest) {
            this.imageRequest = imageRequest;
        }

        @Override
        public ChatResponse complete(ChatRequest request) {
            throw new AssertionError("Text completion should not be called in this test");
        }

        @Override
        public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
            imageRequest.set(request);
            return new ImageGenerationResponse(new byte[]{1, 2, 3}, request.model(), "jpg");
        }
    }

    private interface TextOnlyGateway extends AiGateway {
        @Override
        default ImageGenerationResponse generateImage(ImageGenerationRequest request) {
            throw new AssertionError("Image generation should not be called while building prompts");
        }
    }

    private static class StubImageAssetService extends ImageAssetService {
        @Override
        public SavedImage saveJpeg(byte[] bytes, String purpose) {
            return new SavedImage("assets/images/test.jpg", "/api/v1/assets/images/test.jpg", "/tmp/test.jpg");
        }

        @Override
        public SavedImage savePng(byte[] bytes, String purpose) {
            return new SavedImage("assets/images/test.png", "/api/v1/assets/images/test.png", "/tmp/test.png");
        }
    }
}
