package com.aiwriter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAiImageClientTest {

    private final DefaultAiImageClient client = new DefaultAiImageClient(new ObjectMapper(), HttpClient.newHttpClient());

    @Test
    void buildsPollinationsPromptUrlWithoutApiKey() {
        String url = client.pollinationsImageUrl(
                "https://image.pollinations.ai/prompt",
                "sana",
                "一只红苹果 on a wooden table",
                "1536x1024"
        );

        assertThat(url)
                .startsWith("https://image.pollinations.ai/prompt/")
                .contains("%E4%B8%80%E5%8F%AA%E7%BA%A2%E8%8B%B9%E6%9E%9C%20on%20a%20wooden%20table")
                .contains("width=1536")
                .contains("height=1024")
                .contains("model=sana")
                .contains("nologo=true")
                .contains("nofeed=true")
                .contains("safe=true");
    }
}
