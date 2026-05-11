package com.aiwriter.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAiGatewayTest {

    @Test
    void routesXiaomiTokenPlanRequestsToXiaomiClient() {
        AtomicReference<ChatRequest> xiaomiRequest = new AtomicReference<>();
        AiClient xiaomi = request -> {
            xiaomiRequest.set(request);
            return new ChatResponse("xiaomi", request.model());
        };
        AiClient langchain = request -> {
            throw new AssertionError("Generic provider should not receive Xiaomi Token Plan requests");
        };

        DefaultAiGateway gateway = new DefaultAiGateway(langchain, xiaomi, request -> {
            throw new AssertionError("Image provider should not receive text requests");
        });
        ChatResponse response = gateway.complete(request("https://token-plan-cn.xiaomimimo.com/v1"));

        assertThat(response.text()).isEqualTo("xiaomi");
        assertThat(xiaomiRequest.get()).isNotNull();
    }

    @Test
    void routesGenericRequestsToLangchainClient() {
        AtomicReference<ChatRequest> genericRequest = new AtomicReference<>();
        AiClient langchain = request -> {
            genericRequest.set(request);
            return new ChatResponse("generic", request.model());
        };
        AiClient xiaomi = request -> {
            throw new AssertionError("Xiaomi provider should not receive generic requests");
        };

        DefaultAiGateway gateway = new DefaultAiGateway(langchain, xiaomi, request -> {
            throw new AssertionError("Image provider should not receive text requests");
        });
        ChatResponse response = gateway.complete(request("https://api.deepseek.com/v1"));

        assertThat(response.text()).isEqualTo("generic");
        assertThat(genericRequest.get()).isNotNull();
    }

    @Test
    void routesImageGenerationToImageClient() {
        AtomicReference<ImageGenerationRequest> imageRequest = new AtomicReference<>();
        DefaultAiGateway gateway = new DefaultAiGateway(
                request -> {
                    throw new AssertionError("Text provider should not receive image requests");
                },
                request -> {
                    throw new AssertionError("Text provider should not receive image requests");
                },
                request -> {
                    imageRequest.set(request);
                    return new ImageGenerationResponse(new byte[]{1}, request.model(), "jpg");
                }
        );

        ImageGenerationResponse response = gateway.generateImage(new ImageGenerationRequest(
                "",
                "https://image.pollinations.ai",
                "sana",
                "cat",
                "1024x1024"
        ));

        assertThat(response.format()).isEqualTo("jpg");
        assertThat(imageRequest.get().prompt()).isEqualTo("cat");
    }

    private ChatRequest request(String baseUrl) {
        return new ChatRequest(
                "key",
                baseUrl,
                "model",
                List.of(new ChatMessage("user", "hello")),
                0.2,
                2048
        );
    }
}
