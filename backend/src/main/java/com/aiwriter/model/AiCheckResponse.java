package com.aiwriter.model;

import java.util.List;

public class AiCheckResponse {
    private String summary;
    private String status;
    private boolean aiReviewed;
    private String model;
    private List<AiCheckIssue> issues;
    private String aiReview;

    public AiCheckResponse() {
    }

    public AiCheckResponse(
            String summary,
            String status,
            boolean aiReviewed,
            String model,
            List<AiCheckIssue> issues,
            String aiReview
    ) {
        this.summary = summary;
        this.status = status;
        this.aiReviewed = aiReviewed;
        this.model = model;
        this.issues = issues;
        this.aiReview = aiReview;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAiReviewed() {
        return aiReviewed;
    }

    public void setAiReviewed(boolean aiReviewed) {
        this.aiReviewed = aiReviewed;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<AiCheckIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<AiCheckIssue> issues) {
        this.issues = issues;
    }

    public String getAiReview() {
        return aiReview;
    }

    public void setAiReview(String aiReview) {
        this.aiReview = aiReview;
    }
}
