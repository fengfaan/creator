package com.aiwriter.service;

public class AiWritingException extends RuntimeException {
    private final int status;

    public AiWritingException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
