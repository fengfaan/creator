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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiContentCheckService {
    private static final List<Rule> RULES = List.of(
            new Rule("绝对化表达", List.of(
                    "最强", "最佳", "最好", "第一", "唯一", "顶级", "国家级", "100%", "百分百",
                    "全国第一", "全球最佳", "独家", "绝版", "顶峰", "极品", "特级", "顶尖",
                    "万能", "史上最强", "绝无仅有", "无人能及", "行业第一", "销量第一"),
                    "违反广告法，改为更克制、可验证的表述"),
            new Rule("医疗功效", List.of(
                    "根治", "治愈", "药到病除", "无副作用",
                    "减肥", "祛斑", "美白", "壮阳", "抗癌", "防癌", "降血糖",
                    "包治", "包治百病", "神效"),
                    "小红书禁止医疗功效承诺，改为经验分享或提示咨询专业人士"),
            new Rule("承诺保证", List.of(
                    "保证有效", "稳赚", "永久有效", "零风险", "绝对有效",
                    "包赚", "包过", "稳赚不赔"),
                    "改为概率、条件或个人经验表达"),
            new Rule("投资理财", List.of(
                    "推荐股票", "理财推荐", "投资回报", "内幕消息", "保本保息", "收益率"),
                    "小红书禁止金融投资推荐，建议删除或改为科普性质"),
            new Rule("联系方式", List.of(
                    "微信号", "加微信", "加VX", "加QQ", "扫码加", "私聊我买",
                    "转账", "支付宝"),
                    "小红书禁止留联系方式导流，请删除"),
            new Rule("外部导购", List.of(
                    "淘宝搜索", "京东搜", "拼多多搜", "闲鱼搜", "复制口令", "领券购买"),
                    "小红书禁止站外导购链接，请删除"),
            new Rule("夸张标题", List.of(
                    "震惊", "震撼来袭", "你绝对不知道", "赶紧看", "出大事了", "千万别", "不看后悔"),
                    "标题党可能被限流，改为更自然的表达")
    );
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)(微信[号:]?\\s*\\S{5,}|加微信|加VX|\\+V|加QQ|QQ[号:]?\\s*\\d{5,}|1[3-9]\\d{9}|扫码加|二维码|私聊我买|支付宝[账号]?\\s*\\S+|邮箱[:：]?\\s*\\S+@\\S+)"
    );
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|\\.com|\\.cn|\\.net|淘宝|天猫|京东|拼多多|闲鱼|小程序码|复制口令|领券|下单|购买链接)"
    );
    private static final Pattern MARKETING_PATTERN = Pattern.compile(
            "(限时|秒杀|全网低价|官方旗舰|点击购买|立即购买|闭眼入|必买|买它|冲就完了|私信领取|主页领取|评论区领取|福利|优惠券|返现)"
    );
    private static final Pattern AI_STYLE_PATTERN = Pattern.compile(
            "(综上所述|总而言之|在当今时代|随着.+发展|本文将|值得一提的是|需要注意的是|不可否认|从多个维度|赋能|闭环|抓手|底层逻辑)"
    );
    private static final Pattern IMAGE_MARKDOWN_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");

    private final ConfigService configService;
    private final AiGateway aiGateway;
    private final PromptLoader prompts;

    public AiContentCheckService(ConfigService configService, AiGateway aiGateway, PromptLoader prompts) {
        this.configService = configService;
        this.aiGateway = aiGateway;
        this.prompts = prompts;
    }

    public List<AiCheckIssue> localCheck(String title, String content) {
        return localRuleIssues(safeText(title), safeText(content), true);
    }

    public AiCheckResponse check(AiCheckRequest request) {
        String title = safeText(request == null ? null : request.getTitle());
        String content = safeText(request == null ? null : request.getContent());
        String platform = normalizePlatform(request == null ? null : request.getPlatform());
        boolean xhs = "小红书".equals(platform);
        List<AiCheckIssue> issues = localRuleIssues(title, content, xhs);
        RiskAssessment risk = assessRisk(platform, issues);

        String apiKey = configValue("ai_api_key", "").trim();
        if (apiKey.isBlank()) {
            return new AiCheckResponse(
                    summaryFor(false, issues, risk, xhs),
                    statusFor(issues, risk),
                    false,
                    "",
                    issues,
                    "未配置 API Key，已完成本地规则检查。",
                    xhs ? risk.score() : null,
                    xhs ? risk.level() : ""
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
                            new ChatMessage("system", prompts.get("content-check/system")),
                            new ChatMessage("user", buildPrompt(platform, title, content, issues))
                    ),
                    0.2,
                    4096
            ));
            return new AiCheckResponse(
                    summaryFor(true, issues, risk, xhs),
                    statusFor(issues, risk),
                    true,
                    response.model(),
                    issues,
                    safeText(response.text()),
                    xhs ? risk.score() : null,
                    xhs ? risk.level() : ""
            );
        } catch (AiClientException e) {
            throw new AiWritingException(e.getStatus(), e.getMessage());
        }
    }

    private List<AiCheckIssue> localRuleIssues(String title, String content, boolean includeXhsRisk) {
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
        if (includeXhsRisk) {
            addXhsRiskIssues(title, content, lines, issues);
        }
        return issues;
    }

    private void addXhsRiskIssues(String title, String content, String[] lines, List<AiCheckIssue> issues) {
        if (title.length() > 20) {
            issues.add(new AiCheckIssue(
                    "warn",
                    "标题长度",
                    title.length() + "字",
                    1,
                    title,
                    "小红书标题建议不超过 20 字，请压缩到一个明确钩子"
            ));
        }
        if (content.length() > 1000) {
            issues.add(new AiCheckIssue(
                    "warn",
                    "正文长度",
                    content.length() + "字",
                    2,
                    excerpt(content),
                    "小红书正文建议不超过 1000 字，优先保留核心观点和行动建议"
            ));
        }
        scanPattern(lines, issues, CONTACT_PATTERN, "联系方式", "疑似联系方式", "删除微信、QQ、手机号、二维码、邮箱等导流信息");
        scanPattern(lines, issues, EXTERNAL_LINK_PATTERN, "外部链接/导购", "疑似外链", "删除站外链接、平台名导购、购买口令或领券引导");
        scanPattern(lines, issues, MARKETING_PATTERN, "营销词", "疑似营销表达", "降低销售导向，改成真实体验、步骤或避坑建议");
        scanPattern(lines, issues, AI_STYLE_PATTERN, "AI味提示", "模板化表达", "改成更具体的个人观察、场景细节和短句表达");
        addRepetitionIssues(lines, issues);
        addImageOcrIssues(content, issues);
    }

    private void scanPattern(String[] lines, List<AiCheckIssue> issues, Pattern pattern, String category, String term, String suggestion) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isBlank() && pattern.matcher(line).find()) {
                issues.add(new AiCheckIssue("warn", category, term, i + 1, line, suggestion));
            }
        }
    }

    private void addRepetitionIssues(String[] lines, List<AiCheckIssue> issues) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String normalized = lines[i].replaceAll("[\\p{Punct}\\s，。！？、：；“”‘’（）【】《》#]+", "").trim();
            if (normalized.length() < 12) {
                continue;
            }
            if (!seen.add(normalized)) {
                issues.add(new AiCheckIssue(
                        "warn",
                        "重复度",
                        "重复段落",
                        i + 1,
                        lines[i].trim(),
                        "多篇或多段结构重复容易被判低质，请改写为更具体的案例或删减重复内容"
                ));
            }
        }
    }

    private void addImageOcrIssues(String content, List<AiCheckIssue> issues) {
        java.util.regex.Matcher matcher = IMAGE_MARKDOWN_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
            String alt = matcher.group(1);
            String path = matcher.group(2);
            String imageText = alt + " " + path;
            String suggestion = "发布前请人工检查图片，确保没有二维码、微信号、手机号、外部平台名、水印或大段可读文字";
            String level = CONTACT_PATTERN.matcher(imageText).find() || EXTERNAL_LINK_PATTERN.matcher(imageText).find() ? "warn" : "ok";
            issues.add(new AiCheckIssue(
                    level,
                    "图片OCR风险",
                    "图片复核",
                    lineOf(content, matcher.start()),
                    matcher.group(0),
                    suggestion
            ));
        }
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
        String template = prompts.get("content-check/user-template");
        return PromptLoader.format(template, Map.of(
                "platform", platform,
                "title", title.isBlank() ? "未命名文章" : title,
                "content", content.isBlank() ? "暂无正文" : content,
                "localSummary", localSummary
        ));
    }

    private String summaryFor(boolean aiReviewed, List<AiCheckIssue> issues, RiskAssessment risk, boolean xhs) {
        if (!xhs) {
            if (issues.isEmpty()) {
                return aiReviewed ? "AI 检查完成，未命中本地风险词" : "本地检查完成，未命中风险词";
            }
            return "%s，发现 %d 处本地规则风险".formatted(aiReviewed ? "AI 检查完成" : "本地检查完成", issues.size());
        }
        if (issues.stream().noneMatch(issue -> !"ok".equals(issue.getLevel()))) {
            return "%s，风控评分 %d（%s），未命中阻断风险".formatted(
                    aiReviewed ? "AI 检查完成" : "本地检查完成",
                    risk.score(),
                    risk.level()
            );
        }
        long riskCount = issues.stream().filter(issue -> !"ok".equals(issue.getLevel())).count();
        return "%s，风控评分 %d（%s），发现 %d 处本地规则风险".formatted(
                aiReviewed ? "AI 检查完成" : "本地检查完成",
                risk.score(),
                risk.level(),
                riskCount
        );
    }

    private String statusFor(List<AiCheckIssue> issues, RiskAssessment risk) {
        if (risk.score() < 60 || issues.stream().anyMatch(issue -> "error".equals(issue.getLevel()))) {
            return "error";
        }
        if (risk.score() < 85 || issues.stream().anyMatch(issue -> "warn".equals(issue.getLevel()))) {
            return "warn";
        }
        return "ok";
    }

    private RiskAssessment assessRisk(String platform, List<AiCheckIssue> issues) {
        int score = 100;
        for (AiCheckIssue issue : issues) {
            if ("ok".equals(issue.getLevel())) {
                score -= 2;
                continue;
            }
            score -= switch (issue.getCategory()) {
                case "联系方式", "外部导购", "外部链接/导购" -> 25;
                case "标题长度", "正文长度", "医疗功效", "投资理财" -> 12;
                case "营销词", "重复度", "图片OCR风险" -> 8;
                case "AI味提示", "夸张标题" -> 6;
                default -> 5;
            };
        }
        score = Math.max(0, Math.min(100, score));
        String level = score >= 85 ? "低风险" : score >= 60 ? "中风险" : "高风险";
        return new RiskAssessment(score, level);
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

    private String excerpt(String text) {
        String value = safeText(text).replaceAll("\\s+", " ");
        return value.length() <= 80 ? value : value.substring(0, 80) + "...";
    }

    private int lineOf(String content, int charOffset) {
        int line = 1;
        for (int i = 0; i < charOffset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private record Rule(String label, List<String> terms, String suggestion) {
    }

    private record RiskAssessment(int score, String level) {
    }
}
