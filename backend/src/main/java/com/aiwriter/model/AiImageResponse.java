package com.aiwriter.model;

public class AiImageResponse {
    private String markdown;
    private String assetPath;
    private String url;
    private String filePath;
    private String prompt;
    private String alt;
    private String caption;
    private String model;

    public AiImageResponse() {
    }

    public AiImageResponse(
            String markdown,
            String assetPath,
            String url,
            String filePath,
            String prompt,
            String alt,
            String caption,
            String model
    ) {
        this.markdown = markdown;
        this.assetPath = assetPath;
        this.url = url;
        this.filePath = filePath;
        this.prompt = prompt;
        this.alt = alt;
        this.caption = caption;
        this.model = model;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
