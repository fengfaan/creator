package com.aiwriter.ai;

public class AiClientException extends RuntimeException {
    private final int status;

    public AiClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
