package com.aiwriter.model;

public class AiGenerateResponse {
    private String text;
    private String model;

    public AiGenerateResponse() {
    }

    public AiGenerateResponse(String text, String model) {
        this.text = text;
        this.model = model;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
