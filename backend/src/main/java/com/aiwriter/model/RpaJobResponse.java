package com.aiwriter.model;

public class RpaJobResponse {
    private String jobId;
    private String platform;
    private String status;
    private String message;

    public RpaJobResponse() {
    }

    public RpaJobResponse(String jobId, String platform, String status, String message) {
        this.jobId = jobId;
        this.platform = platform;
        this.status = status;
        this.message = message;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
