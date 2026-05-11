package com.aiwriter.service;

import com.aiwriter.model.AiGenerateRequest;
import org.springframework.stereotype.Component;

@Component
class AiWritingPromptFactory {

    AiWritingPrompt build(AiWritingAction action, AiGenerateRequest request) {
        return switch (action) {
            case OUTLINE -> new AiWritingPrompt(
                    "你是资深中文内容策划。请输出结构清晰、可直接写作的 Markdown 大纲。",
                    context(request) + "\n请生成一份适合继续写作的 Markdown 文章大纲。",
                    0.55,
                    4096
            );
            case DRAFT -> new AiWritingPrompt(
                    "你是中文长文主笔。请严格依据用户提供的大纲扩写成完整正文，不要偏离大纲层级和论证顺序。",
                    context(request) + """
                        请基于上方大纲生成完整 Markdown 正文。
                        要求：
                        - 必须覆盖大纲中的每一个一级/二级条目，不要只写前半部分。
                        - 每个主要小节至少扩写 2-4 段。
                        - 如果篇幅较长，优先保证所有章节都有完整内容，而不是在前几节写得过长。
                        - 直接输出正文，不要重复输出大纲。
                        """,
                    0.72,
                    8192
            );
            case POLISH -> new AiWritingPrompt(
                    "你是中文写作编辑。请在保留事实和核心观点的前提下，让表达更清晰、有节奏、有传播感。",
                    context(request) + "\n请结合大纲检查结构一致性，并润色当前正文。直接输出润色后的完整正文。",
                    0.75,
                    8192
            );
            case CONTINUE -> new AiWritingPrompt(
                    "你是中文长文写作助手。请自然衔接上文，续写一段或数段内容，不要重复已有内容。",
                    context(request) + "\n请参考大纲续写后续内容，直接输出可插入正文的 Markdown 文本。",
                    0.75,
                    4096
            );
        };
    }

    private String context(AiGenerateRequest request) {
        String title = safeText(request == null ? null : request.getTitle());
        String outline = safeText(request == null ? null : request.getOutline());
        String content = safeText(request == null ? null : request.getContent());
        return """
            标题：
            %s

            大纲：
            %s

            当前正文：
            %s
            """.formatted(
                title.isBlank() ? "未命名文章" : title,
                outline.isBlank() ? "暂无大纲" : outline,
                content.isBlank() ? "暂无正文" : content
            );
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
