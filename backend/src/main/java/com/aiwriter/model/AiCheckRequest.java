package com.aiwriter.model;

public class AiCheckRequest {
    private String platform;
    private String title;
    private String content;

    public AiCheckRequest() {
    }

    public AiCheckRequest(String platform, String title, String content) {
        this.platform = platform;
        this.title = title;
        this.content = content;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public void setContent(String content) {
        this.content = content;
    }
}
