package com.aiwriter.model;

public class RpaPublishRequest {
    private String platform;
    private String title;
    private String content;
    private String coverPath;

    public RpaPublishRequest() {
    }

    public RpaPublishRequest(String platform, String title, String content, String coverPath) {
        this.platform = platform;
        this.title = title;
        this.content = content;
        this.coverPath = coverPath;
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

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
}
