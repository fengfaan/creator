package com.aiwriter.rpa;

public class RpaException extends RuntimeException {
    private final int status;

    public RpaException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
