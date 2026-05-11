package com.aiwriter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class XiaomiTokenPlanAiClientTest {

    @Test
    void buildsAnthropicMessagesUrlFromXiaomiBaseUrl() {
        XiaomiTokenPlanAiClient client = new XiaomiTokenPlanAiClient(new ObjectMapper(), HttpClient.newHttpClient());

        assertThat(client.messagesUrl("https://token-plan-cn.xiaomimimo.com/v1"))
                .isEqualTo("https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages");
        assertThat(client.messagesUrl("https://token-plan-cn.xiaomimimo.com/anthropic"))
                .isEqualTo("https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages");
        assertThat(client.messagesUrl("https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages"))
                .isEqualTo("https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages");
    }
}
