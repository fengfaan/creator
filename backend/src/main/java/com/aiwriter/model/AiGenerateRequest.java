package com.aiwriter.model;

public class AiGenerateRequest {
    private String action;
    private String title;
    private String outline;
    private String content;

    public AiGenerateRequest() {
    }

    public AiGenerateRequest(String action, String title, String content) {
        this.action = action;
        this.title = title;
        this.content = content;
    }

    public AiGenerateRequest(String action, String title, String outline, String content) {
        this.action = action;
        this.title = title;
        this.outline = outline;
        this.content = content;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
