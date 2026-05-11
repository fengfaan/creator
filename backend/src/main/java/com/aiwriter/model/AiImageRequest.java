package com.aiwriter.model;

public class AiImageRequest {
    private String purpose;
    private String title;
    private String content;
    private String referenceText;

    public AiImageRequest() {
    }

    public AiImageRequest(String purpose, String title, String content, String referenceText) {
        this.purpose = purpose;
        this.title = title;
        this.content = content;
        this.referenceText = referenceText;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public String getReferenceText() {
        return referenceText;
    }

    public void setReferenceText(String referenceText) {
        this.referenceText = referenceText;
    }
}
