package com.aiwriter.service;

enum AiWritingAction {
    OUTLINE("outline"),
    DRAFT("draft"),
    POLISH("polish"),
    CONTINUE("continue");

    private final String value;

    AiWritingAction(String value) {
        this.value = value;
    }

    static AiWritingAction from(String value) {
        if (value == null || value.isBlank()) {
            throw new AiWritingException(400, "AI 动作不能为空");
        }
        String normalized = value.trim().toLowerCase();
        for (AiWritingAction action : values()) {
            if (action.value.equals(normalized)) {
                return action;
            }
        }
        throw new AiWritingException(400, "不支持的 AI 动作");
    }
}
