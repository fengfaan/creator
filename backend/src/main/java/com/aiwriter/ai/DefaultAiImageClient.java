package com.aiwriter.ai;

import com.aiwriter.service.AiWritingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultAiImageClient implements AiImageClient {
    static final String POLLINATIONS_IMAGE_BASE_URL = "https://image.pollinations.ai";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DefaultAiImageClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    DefaultAiImageClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ImageGenerationResponse generate(ImageGenerationRequest request) {
        if (usesPollinations(request.baseUrl())) {
            return generatePollinationsImage(request);
        }
        return new ImageGenerationResponse(
                createOpenAiCompatibleImage(request),
                request.model(),
                "png"
        );
    }

    private ImageGenerationResponse generatePollinationsImage(ImageGenerationRequest request) {
        String safePrompt = pollinationsSafePrompt(request.prompt());
        try {
            return new ImageGenerationResponse(
                    downloadImage(pollinationsImageUrl(request.baseUrl(), request.model(), safePrompt, request.size())),
                    request.model(),
                    "jpg"
            );
        } catch (AiWritingException e) {
            String fallbackSize = pollinationsFallbackSize(request.size());
            if (fallbackSize.equals(request.size())) {
                throw e;
            }
            return new ImageGenerationResponse(
                    downloadImage(pollinationsImageUrl(request.baseUrl(), request.model(), safePrompt, fallbackSize)),
                    request.model(),
                    "jpg"
            );
        }
    }

    private byte[] createOpenAiCompatibleImage(ImageGenerationRequest generationRequest) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", generationRequest.model());
        payload.put("prompt", generationRequest.prompt());
        payload.put("size", generationRequest.size());
        payload.put("n", 1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageGenerationUrl(generationRequest.baseUrl())))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + generationRequest.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiWritingException(502, upstreamErrorMessage(response.body(), response.statusCode()));
            }
            return extractImageBytes(response.body());
        } catch (AiWritingException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new AiWritingException(400, "图片 Base URL 格式不正确");
        } catch (IOException e) {
            throw new AiWritingException(502, "图片服务连接失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiWritingException(502, "图片请求已中断");
        }
    }

    String imageGenerationUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? POLLINATIONS_IMAGE_BASE_URL : baseUrl.trim();
        String trimmed = value.replaceAll("/+$", "");
        if (trimmed.endsWith("/images/generations")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/images/generations";
        }
        return trimmed + "/v1/images/generations";
    }

    String pollinationsImageUrl(String baseUrl, String model, String prompt, String size) {
        String[] dimensions = parseSize(size);
        String normalizedBaseUrl = normalizePollinationsBaseUrl(baseUrl);
        String encodedPrompt = URLEncoder.encode(pollinationsSafePrompt(prompt), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String safeModel = model == null || model.isBlank() ? "sana" : model.trim();
        return normalizedBaseUrl
                + "/prompt/" + encodedPrompt
                + "?width=" + dimensions[0]
                + "&height=" + dimensions[1]
                + "&model=" + urlParam(safeModel)
                + "&nologo=true"
                + "&nofeed=true"
                + "&safe=true";
    }

    boolean usesPollinations(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim().toLowerCase(Locale.ROOT);
        return value.isBlank()
                || value.equals("pollinations")
                || value.contains("image.pollinations.ai");
    }

    private String pollinationsSafePrompt(String prompt) {
        String compact = safeText(prompt)
                .replaceAll("(?m)^#+\\s*", "")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (compact.length() <= 900) {
            return compact;
        }
        return compact.substring(0, 900).replaceAll("\\s+\\S*$", "").trim()
                + ". No readable text, no logos, no watermarks.";
    }

    private String pollinationsFallbackSize(String size) {
        String[] dimensions = parseSize(size);
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        if (height > width) {
            return "768x1024";
        }
        if (width > height) {
            return "1024x768";
        }
        return width > 768 ? "768x768" : size;
    }

    private String normalizePollinationsBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? POLLINATIONS_IMAGE_BASE_URL : baseUrl.trim();
        if (value.equalsIgnoreCase("pollinations")) {
            return POLLINATIONS_IMAGE_BASE_URL;
        }
        String trimmed = value.replaceAll("/+$", "");
        if (trimmed.endsWith("/prompt")) {
            return trimmed.substring(0, trimmed.length() - "/prompt".length());
        }
        return trimmed;
    }

    private byte[] extractImageBytes(String responseBody) throws IOException {
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new AiWritingException(502, "图片服务返回内容为空");
        }
        JsonNode first = data.get(0);
        String b64 = first.path("b64_json").asText("");
        if (!b64.isBlank()) {
            return Base64.getDecoder().decode(b64);
        }
        String url = first.path("url").asText("");
        if (!url.isBlank()) {
            return downloadImage(url);
        }
        throw new AiWritingException(502, "图片服务没有返回可保存的图片");
    }

    private byte[] downloadImage(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiWritingException(502, "图片下载失败 (" + response.statusCode() + ")");
            }
            return response.body();
        } catch (AiWritingException e) {
            throw e;
        } catch (IOException e) {
            return downloadImageWithCurl(url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiWritingException(502, "图片下载已中断");
        }
    }

    private byte[] downloadImageWithCurl(String url, IOException cause) {
        try {
            Process process = new ProcessBuilder(
                    "curl",
                    "-L",
                    "--fail",
                    "--silent",
                    "--show-error",
                    "--max-time",
                    "60",
                    url
            ).start();
            boolean finished = process.waitFor(70, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AiWritingException(502, "图片下载失败: Pollinations 生成超时，请稍后重试或换一个更短的标题");
            }
            byte[] body = process.getInputStream().readAllBytes();
            byte[] error = process.getErrorStream().readAllBytes();
            if (process.exitValue() != 0) {
                String message = new String(error, StandardCharsets.UTF_8).trim();
                throw new AiWritingException(502, "图片下载失败: " + blankAs(message, cause.getMessage()));
            }
            if (body.length == 0) {
                throw new AiWritingException(502, "图片下载失败: 返回图片为空");
            }
            return body;
        } catch (AiWritingException e) {
            throw e;
        } catch (IOException e) {
            throw new AiWritingException(502, "图片下载失败: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiWritingException(502, "图片下载已中断");
        }
    }

    private String[] parseSize(String size) {
        String value = size == null ? "" : size.trim().toLowerCase(Locale.ROOT);
        String[] parts = value.split("x", 2);
        if (parts.length == 2 && isPositiveInt(parts[0]) && isPositiveInt(parts[1])) {
            return parts;
        }
        return new String[]{"1024", "1024"};
    }

    private boolean isPositiveInt(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String urlParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String upstreamErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (message.isBlank()) {
                message = root.path("message").asText("");
            }
            if (!message.isBlank()) {
                return "图片服务返回错误 (" + statusCode + "): " + message;
            }
        } catch (Exception ignored) {
        }
        return "图片服务返回错误 (" + statusCode + ")";
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
