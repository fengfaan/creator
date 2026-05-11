package com.aiwriter.service;

import com.aiwriter.ai.ChatResponse;
import com.aiwriter.ai.ImageGenerationResponse;
import com.aiwriter.model.AiCheckRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiContentCheckServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private AiContentCheckService service;

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
        service = new AiContentCheckService(configService, (TextOnlyGateway) request -> {
            throw new AssertionError("AI gateway should not be called without an API key");
        }, new PromptLoader(configService));
    }

    @Test
    void returnsLocalRuleIssuesWithoutApiKey() {
        var response = service.check(new AiCheckRequest("wechat", "最好的方案", "这个方法绝对有效"));

        assertThat(response.isAiReviewed()).isFalse();
        assertThat(response.getStatus()).isEqualTo("warn");
        assertThat(response.getIssues()).extracting("term")
                .contains("最好", "绝对有效");
        assertThat(response.getRiskScore()).isNull();
        assertThat(response.getRiskLevel()).isBlank();
        assertThat(response.getAiReview()).contains("未配置 API Key");
    }

    @Test
    void wechatCheckDoesNotAddXhsSpecificRiskIssues() {
        var response = service.check(new AiCheckRequest(
                "wechat",
                "这是一个超过二十字但公众号不应触发小红书标题检查",
                "正文 " + "内容".repeat(600)
        ));

        assertThat(response.getRiskScore()).isNull();
        assertThat(response.getIssues()).extracting("category")
                .doesNotContain("标题长度", "正文长度", "AI味提示", "图片OCR风险");
    }

    @Test
    void xhsCheckAddsPrePublishRiskIssues() {
        String longTitle = "这是一个超过二十字的小红书标题需要压缩一下";
        String content = """
                随着时代发展，本文将从多个维度告诉你这套方案。
                加微信 abcdefg 领取福利，淘宝搜索同款，立即购买。
                这个段落用于测试重复内容风险触发机制
                这个段落用于测试重复内容风险触发机制
                ![微信二维码](assets/images/cover.png)
                """;

        var response = service.check(new AiCheckRequest("xhs", longTitle, content));

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getRiskLevel()).isEqualTo("高风险");
        assertThat(response.getIssues()).extracting("category")
                .contains("标题长度", "联系方式", "外部链接/导购", "营销词", "重复度", "AI味提示", "图片OCR风险");
    }

    @Test
    void callsAiGatewayWhenApiKeyConfigured() {
        configService.set("ai_api_key", "test-key");
        AtomicReference<String> prompt = new AtomicReference<>();
        service = new AiContentCheckService(configService, (TextOnlyGateway) request -> {
            prompt.set(request.messages().get(1).content());
            return new ChatResponse("整体可发布，建议弱化绝对化表达。", request.model());
        }, new PromptLoader(configService));

        var response = service.check(new AiCheckRequest("xhs", "标题", "正文"));

        assertThat(response.isAiReviewed()).isTrue();
        assertThat(response.getModel()).isEqualTo(AiWritingService.DEFAULT_MODEL);
        assertThat(response.getAiReview()).contains("整体可发布");
        assertThat(prompt.get()).contains("发布平台：小红书");
    }

    private interface TextOnlyGateway extends com.aiwriter.ai.AiGateway {
        @Override
        default ImageGenerationResponse generateImage(com.aiwriter.ai.ImageGenerationRequest request) {
            throw new AssertionError("Image generation should not be called in content check tests");
        }
    }
}
