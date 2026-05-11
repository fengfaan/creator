package com.aiwriter.model;

public class RpaLogEntry {
    private long sequence;
    private String time;
    private String level;
    private String message;

    public RpaLogEntry() {
    }

    public RpaLogEntry(long sequence, String time, String level, String message) {
        this.sequence = sequence;
        this.time = time;
        this.level = level;
        this.message = message;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
