package com.aiwriter.service;

import com.aiwriter.ai.ChatResponse;
import com.aiwriter.ai.ImageGenerationResponse;
import com.aiwriter.model.AiGenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiWritingServiceTest {

    @TempDir
    Path tempDir;

    private AiWritingService service;
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
        PromptLoader promptLoader = new PromptLoader(configService);
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(promptLoader),
                (TextOnlyGateway) request -> {
                    throw new AssertionError("AI gateway should not be called without an API key");
                }
        );
    }

    @Test
    void requiresApiKeyBeforeCallingAiGateway() {
        AiGenerateRequest request = new AiGenerateRequest("outline", "标题", "正文");

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(AiWritingException.class)
                .hasMessage("请先在设置中配置 API Key")
                .satisfies(e -> assertThat(((AiWritingException) e).getStatus()).isEqualTo(400));
    }

    @Test
    void normalizesXiaomiDisplayModelBeforeCallingAiGateway() {
        configService.set("ai_api_key", "test-key");
        configService.set("ai_base_url", "https://token-plan-cn.xiaomimimo.com/v1");
        configService.set("selected_model", "MiMo-V2.5-Pro");
        AtomicReference<String> model = new AtomicReference<>();
        PromptLoader promptLoader = new PromptLoader(configService);
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(promptLoader),
                (TextOnlyGateway) request -> {
                    model.set(request.model());
                    return new ChatResponse("ok", request.model());
                }
        );

        service.generate(new AiGenerateRequest("outline", "标题", "正文"));

        assertThat(model.get()).isEqualTo("mimo-v2.5-pro");
    }

    @Test
    void draftPromptIncludesOutline() {
        configService.set("ai_api_key", "test-key");
        AtomicReference<String> userPrompt = new AtomicReference<>();
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(new PromptLoader(configService)),
                (TextOnlyGateway) request -> {
                    userPrompt.set(request.messages().get(1).content());
                    return new ChatResponse("正文", request.model());
                }
        );

        service.generate(new AiGenerateRequest("draft", "标题", "1. 起因\n2. 方案", ""));

        assertThat(userPrompt.get())
                .contains("大纲：")
                .contains("1. 起因")
                .contains("基于上方大纲生成完整 Markdown 正文");
    }

    @Test
    void promptIncludesUserInstruction() {
        configService.set("ai_api_key", "test-key");
        AtomicReference<String> userPrompt = new AtomicReference<>();
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(new PromptLoader(configService)),
                (TextOnlyGateway) request -> {
                    userPrompt.set(request.messages().get(1).content());
                    return new ChatResponse("正文", request.model());
                }
        );

        service.generate(new AiGenerateRequest(
                "polish",
                "标题",
                "",
                "正文",
                "改成小红书风格，正文不超过 1000 字，改进建议分点输出。"
        ));

        assertThat(userPrompt.get())
                .contains("用户额外要求（必须优先遵守）")
                .contains("正文不超过 1000 字")
                .contains("改进建议分点输出");
    }

    @Test
    void xhsPlatformUsesXhsSpecificPrompts() {
        configService.set("ai_api_key", "test-key");
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AtomicReference<String> userPrompt = new AtomicReference<>();
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(new PromptLoader(configService)),
                (TextOnlyGateway) request -> {
                    systemPrompt.set(request.messages().get(0).content());
                    userPrompt.set(request.messages().get(1).content());
                    return new ChatResponse("正文", request.model());
                }
        );

        AiGenerateRequest xhsRequest = new AiGenerateRequest("draft", "标题", "大纲内容", "正文内容");
        xhsRequest.setPlatform("xhs");
        service.generate(xhsRequest);

        assertThat(systemPrompt.get()).contains("小红书");
        assertThat(userPrompt.get()).contains("小红书笔记");
    }

    @Test
    void defaultPlatformUsesGenericPrompts() {
        configService.set("ai_api_key", "test-key");
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        service = new AiWritingService(
                configService,
                new AiWritingPromptFactory(new PromptLoader(configService)),
                (TextOnlyGateway) request -> {
                    systemPrompt.set(request.messages().get(0).content());
                    return new ChatResponse("正文", request.model());
                }
        );

        service.generate(new AiGenerateRequest("draft", "标题", "大纲内容", "正文内容"));

        assertThat(systemPrompt.get()).doesNotContain("小红书");
        assertThat(systemPrompt.get()).contains("中文长文主笔");
    }

    private interface TextOnlyGateway extends com.aiwriter.ai.AiGateway {
        @Override
        default ImageGenerationResponse generateImage(com.aiwriter.ai.ImageGenerationRequest request) {
            throw new AssertionError("Image generation should not be called in writing service tests");
        }
    }
}
