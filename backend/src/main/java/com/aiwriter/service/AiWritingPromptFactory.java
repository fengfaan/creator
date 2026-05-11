package com.aiwriter.service;

import com.aiwriter.model.AiGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class AiWritingPromptFactory {

    private final PromptLoader prompts;

    AiWritingPromptFactory(PromptLoader prompts) {
        this.prompts = prompts;
    }

    AiWritingPrompt build(AiWritingAction action, AiGenerateRequest request) {
        String prefix = resolvePrefix(request);
        return switch (action) {
            case OUTLINE -> new AiWritingPrompt(
                    prompts.get(prefix + "outline-system"),
                    buildUser(prefix + "outline-user", request),
                    0.55,
                    4096
            );
            case DRAFT -> new AiWritingPrompt(
                    prompts.get(prefix + "draft-system"),
                    buildUser(prefix + "draft-user", request),
                    0.72,
                    8192
            );
            case POLISH -> new AiWritingPrompt(
                    prompts.get(prefix + "polish-system"),
                    buildUser(prefix + "polish-user", request),
                    0.75,
                    8192
            );
            case CONTINUE -> new AiWritingPrompt(
                    prompts.get(prefix + "continue-system"),
                    buildUser(prefix + "continue-user", request),
                    0.75,
                    4096
            );
        };
    }

    private String resolvePrefix(AiGenerateRequest request) {
        if (request != null && "xhs".equalsIgnoreCase(request.getPlatform())) {
            return "writing-xhs/";
        }
        return "writing/";
    }

    private String buildUser(String templateName, AiGenerateRequest request) {
        String context = context(request);
        String instruction = safeText(request == null ? null : request.getInstruction());
        String template = prompts.get(templateName);
        return PromptLoader.format(template, Map.of(
                "context", context,
                "instruction", instruction.isBlank() ? "" : "\n用户额外要求（必须优先遵守）：" + instruction
        ));
    }

    private String context(AiGenerateRequest request) {
        String title = safeText(request == null ? null : request.getTitle());
        String outline = safeText(request == null ? null : request.getOutline());
        String content = safeText(request == null ? null : request.getContent());
        String template = prompts.get(resolvePrefix(request) + "context");
        return PromptLoader.format(template, Map.of(
                "title", title.isBlank() ? "未命名文章" : title,
                "outline", outline.isBlank() ? "暂无大纲" : outline,
                "content", content.isBlank() ? "暂无正文" : content
        ));
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
