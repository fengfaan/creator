package com.aiwriter.service;

import com.aiwriter.ai.AiClientException;
import com.aiwriter.ai.AiGateway;
import com.aiwriter.ai.ChatMessage;
import com.aiwriter.ai.ChatRequest;
import com.aiwriter.ai.ChatResponse;
import com.aiwriter.model.AiCheckIssue;
import com.aiwriter.model.AiCheckRequest;
import com.aiwriter.model.AiCheckResponse;
import com.aiwriter.model.ConfigItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiContentCheckService {
    private static final List<Rule> RULES = List.of(
            new Rule("绝对化表达", List.of("最强", "最佳", "最好", "第一", "唯一", "顶级", "国家级", "100%", "百分百"),
                    "改为更克制、可验证的表述"),
            new Rule("医疗功效", List.of("根治", "治愈", "药到病除", "无副作用"),
                    "避免承诺医疗效果，改为经验描述或提示咨询专业人士"),
            new Rule("承诺保证", List.of("保证有效", "稳赚", "永久有效", "零风险", "绝对有效"),
                    "改为概率、条件或个人经验表达")
    );

    private final ConfigService configService;
    private final AiGateway aiGateway;

    public AiContentCheckService(ConfigService configService, AiGateway aiGateway) {
        this.configService = configService;
        this.aiGateway = aiGateway;
    }

    public AiCheckResponse check(AiCheckRequest request) {
        String title = safeText(request == null ? null : request.getTitle());
        String content = safeText(request == null ? null : request.getContent());
        String platform = normalizePlatform(request == null ? null : request.getPlatform());
        List<AiCheckIssue> issues = localRuleIssues(title, content);

        String apiKey = configValue("ai_api_key", "").trim();
        if (apiKey.isBlank()) {
            return new AiCheckResponse(
                    summaryFor(false, issues),
                    issues.isEmpty() ? "ok" : "warn",
                    false,
                    "",
                    issues,
                    "未配置 API Key，已完成本地规则检查。"
            );
        }

        String baseUrl = configValue("ai_base_url", AiWritingService.DEFAULT_BASE_URL).trim();
        String model = configValue("selected_model", AiWritingService.DEFAULT_MODEL).trim();
        String resolvedBaseUrl = baseUrl.isBlank() ? AiWritingService.DEFAULT_BASE_URL : baseUrl;
        String resolvedModel = normalizeModel(model, resolvedBaseUrl);

        try {
            ChatResponse response = aiGateway.complete(new ChatRequest(
                    apiKey,
                    resolvedBaseUrl,
                    resolvedModel,
                    List.of(
                            new ChatMessage("system", "你是中文新媒体内容合规审稿人。请重点检查夸大宣传、医疗功效承诺、绝对化用语、平台发布风险和表达可读性。"),
                            new ChatMessage("user", buildPrompt(platform, title, content, issues))
                    ),
                    0.2,
                    4096
            ));
            return new AiCheckResponse(
                    summaryFor(true, issues),
                    issues.isEmpty() ? "ok" : "warn",
                    true,
                    response.model(),
                    issues,
                    safeText(response.text())
            );
        } catch (AiClientException e) {
            throw new AiWritingException(e.getStatus(), e.getMessage());
        }
    }

    private List<AiCheckIssue> localRuleIssues(String title, String content) {
        String[] lines = ("%s\n%s".formatted(title, content)).split("\\R", -1);
        List<AiCheckIssue> issues = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (Rule rule : RULES) {
                for (String term : rule.terms()) {
                    if (line.contains(term)) {
                        issues.add(new AiCheckIssue(
                                "warn",
                                rule.label(),
                                term,
                                i + 1,
                                line.trim(),
                                rule.suggestion()
                        ));
                    }
                }
            }
        }
        return issues;
    }

    private String buildPrompt(String platform, String title, String content, List<AiCheckIssue> localIssues) {
        String localSummary = localIssues.isEmpty()
                ? "本地规则未命中占位风险词。"
                : String.join("\n", localIssues.stream()
                        .map(issue -> "- 第 %d 行 [%s] 命中「%s」：%s".formatted(
                                issue.getLine(),
                                issue.getCategory(),
                                issue.getTerm(),
                                issue.getExcerpt()
                        ))
                        .toList());
        return """
                发布平台：%s

                标题：
                %s

                正文：
                %s

                本地规则初筛：
                %s

                请用中文输出：
                1. 总体结论，一句话。
                2. 风险点列表，每条包含位置、原因、修改建议。
                3. 如果没有明显风险，请说明可以发布但仍需人工复核。
                不要输出 JSON。
                """.formatted(platform, title.isBlank() ? "未命名文章" : title, content.isBlank() ? "暂无正文" : content, localSummary);
    }

    private String summaryFor(boolean aiReviewed, List<AiCheckIssue> issues) {
        if (issues.isEmpty()) {
            return aiReviewed ? "AI 检查完成，未命中本地风险词" : "本地检查完成，未命中风险词";
        }
        return "%s，发现 %d 处本地规则风险".formatted(aiReviewed ? "AI 检查完成" : "本地检查完成", issues.size());
    }

    private String normalizePlatform(String platform) {
        if ("xhs".equalsIgnoreCase(safeText(platform))) {
            return "小红书";
        }
        return "微信公众号";
    }

    private String normalizeModel(String model, String baseUrl) {
        String value = model == null || model.isBlank() ? AiWritingService.DEFAULT_MODEL : model.trim();
        if (baseUrl != null && baseUrl.toLowerCase().contains("xiaomimimo.com")) {
            return switch (value.toLowerCase()) {
                case "mimo-v2.5-pro" -> "mimo-v2.5-pro";
                case "mimo-v2.5" -> "mimo-v2.5";
                case "mimo-v2-flash" -> "mimo-v2-flash";
                default -> value.replace("MiMo", "mimo").replace("V", "v").toLowerCase();
            };
        }
        return value;
    }

    private String configValue(String key, String fallback) {
        ConfigItem item = configService.get(key);
        if (item == null || item.getValue() == null || item.getValue().isBlank()) {
            return fallback;
        }
        return item.getValue();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record Rule(String label, List<String> terms, String suggestion) {
    }
}
