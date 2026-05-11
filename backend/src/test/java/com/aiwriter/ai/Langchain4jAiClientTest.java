package com.aiwriter.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Langchain4jAiClientTest {

    private final Langchain4jAiClient client = new Langchain4jAiClient();

    @Test
    void normalizesOpenAiBaseUrlForLangchain4jBuilder() {
        assertThat(client.openAiBaseUrl("https://api.example.com/v1"))
                .isEqualTo("https://api.example.com/v1");
        assertThat(client.openAiBaseUrl("https://api.example.com/v1/chat/completions/"))
                .isEqualTo("https://api.example.com/v1");
    }

    @Test
    void normalizesAnthropicBaseUrlForXiaomiTokenPlan() {
        assertThat(client.anthropicBaseUrl("https://token-plan-cn.xiaomimimo.com/v1"))
                .isEqualTo("https://token-plan-cn.xiaomimimo.com/anthropic");
        assertThat(client.anthropicBaseUrl("https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages"))
                .isEqualTo("https://token-plan-cn.xiaomimimo.com/anthropic");
    }

    @Test
    void preservesConversationRolesWhenUsingSkillAwareAssistant() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "你是写作助手"),
                new ChatMessage("user", "画一张图"),
                new ChatMessage("assistant", "需要什么风格？"),
                new ChatMessage("user", "赛博朋克")
        );

        assertThat(client.systemMessages(messages)).isEqualTo("你是写作助手");
        assertThat(client.conversationPrompt(messages))
                .contains("user:\n画一张图")
                .contains("assistant:\n需要什么风格？")
                .contains("user:\n赛博朋克")
                .doesNotContain("system:");
    }
}
