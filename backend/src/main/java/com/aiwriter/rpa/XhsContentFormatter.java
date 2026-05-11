package com.aiwriter.rpa;

import com.aiwriter.model.RpaPublishRequest;

import java.util.regex.Pattern;

final class XhsContentFormatter {
    private static final int XHS_TITLE_LIMIT = 20;
    private static final int XHS_BODY_LIMIT = 1000;
    private static final String[] CIRCLED_NUMBERS = {"①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"};
    private static final Pattern IMAGE_LINE = Pattern.compile("^!\\[[^]]*]\\([^)]+\\)\\s*$");
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern QUOTE = Pattern.compile("^>\\s?(.+)$");
    private static final Pattern UNORDERED = Pattern.compile("^[-*+]\\s+(.+)$");
    private static final Pattern ORDERED = Pattern.compile("^(\\d+)[.)、]\\s+(.+)$");
    private static final Pattern CONTACT_INFO = Pattern.compile(
            "(?i)(微信[号:]?\\s*\\S{5,}|加微信|加VX|加QQ|QQ[号:]?\\s*\\d{5,}|1[3-9]\\d{9}|扫码加|私聊我买|支付宝[账号]?\\s*\\S+)"
    );
    private static final Pattern EXTERNAL_SHOP = Pattern.compile(
            "(?i)(淘宝搜索|京东搜|拼多多搜|闲鱼搜|复制口令|领券购买|tmall\\.com|taobao\\.com|jd\\.com|pinduoduo\\.com)"
    );

    private XhsContentFormatter() {
    }

    static RpaPublishRequest format(RpaPublishRequest request) {
        if (request == null || !"xhs".equalsIgnoreCase(safe(request.getPlatform()))) {
            return request;
        }
        String title = normalizeInline(request.getTitle()).replaceFirst("^#+\\s*", "").trim();
        String content = stripDuplicatedTitle(safe(request.getContent()), title);
        String plainBody = toPlainText(content);
        plainBody = stripContactInfo(plainBody);
        plainBody = stripExternalShop(plainBody);
        plainBody = truncateBody(plainBody, XHS_BODY_LIMIT);
        if (title.length() > XHS_TITLE_LIMIT) {
            title = title.substring(0, XHS_TITLE_LIMIT);
        }
        RpaPublishRequest formatted = new RpaPublishRequest();
        formatted.setPlatform(request.getPlatform());
        formatted.setTitle(title.isBlank() ? "未命名笔记" : title);
        formatted.setContent(plainBody);
        formatted.setCoverPath(request.getCoverPath());
        return formatted;
    }

    private static String toPlainText(String markdown) {
        StringBuilder out = new StringBuilder();
        boolean previousBlank = true;
        String[] lines = safe(markdown).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isBlank()) {
                previousBlank = appendBlank(out, previousBlank);
                continue;
            }
            if (IMAGE_LINE.matcher(line).matches()) {
                previousBlank = appendBlank(out, previousBlank);
                continue;
            }

            java.util.regex.Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                previousBlank = appendBlank(out, previousBlank);
                appendLine(out, formatHeading(heading.group(1).length(), heading.group(2)));
                previousBlank = appendBlank(out, false);
                continue;
            }

            java.util.regex.Matcher quote = QUOTE.matcher(line);
            if (quote.matches()) {
                appendLine(out, "▎" + normalizeInline(quote.group(1)));
                previousBlank = false;
                continue;
            }

            java.util.regex.Matcher unordered = UNORDERED.matcher(line);
            if (unordered.matches()) {
                appendLine(out, "• " + normalizeInline(unordered.group(1)));
                previousBlank = false;
                continue;
            }

            java.util.regex.Matcher ordered = ORDERED.matcher(line);
            if (ordered.matches()) {
                int index = Integer.parseInt(ordered.group(1));
                String marker = index >= 1 && index <= CIRCLED_NUMBERS.length ? CIRCLED_NUMBERS[index - 1] : index + ".";
                appendLine(out, marker + " " + normalizeInline(ordered.group(2)));
                previousBlank = false;
                continue;
            }

            appendLine(out, normalizeInline(line));
            previousBlank = false;
        }
        return out.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String formatHeading(int level, String text) {
        String clean = normalizeInline(text);
        if (level <= 1) {
            return clean;
        }
        if (level == 2) {
            return "📌 " + clean;
        }
        return "—— " + clean + " ——";
    }

    private static String normalizeInline(String text) {
        return safe(text)
                .replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1")
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("__([^_]+)__", "$1")
                .replaceAll("\\*([^*\\n]+)\\*", "$1")
                .replaceAll("_([^_\\n]+)_", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("~~([^~]+)~~", "$1")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }

    private static String stripDuplicatedTitle(String content, String title) {
        if (title == null || title.isBlank()) {
            return content.trim();
        }
        String escaped = Pattern.quote(title.trim());
        return content.replaceFirst("(?i)^#\\s+" + escaped + "\\s*\\n+", "").trim();
    }

    private static void appendLine(StringBuilder out, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        out.append(line.trim());
    }

    private static boolean appendBlank(StringBuilder out, boolean previousBlank) {
        if (!previousBlank && !out.isEmpty()) {
            if (out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append('\n');
            return true;
        }
        return previousBlank;
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    private static String stripContactInfo(String text) {
        return CONTACT_INFO.matcher(text).replaceAll("");
    }

    private static String stripExternalShop(String text) {
        return EXTERNAL_SHOP.matcher(text).replaceAll("");
    }

    private static String truncateBody(String text, int limit) {
        if (text.length() <= limit) return text;
        int cut = text.lastIndexOf('\n', limit);
        if (cut > limit / 2) {
            return text.substring(0, cut).trim();
        }
        int space = text.lastIndexOf(' ', limit);
        if (space > limit / 2) {
            return text.substring(0, space).trim();
        }
        return text.substring(0, limit).trim();
    }
}
