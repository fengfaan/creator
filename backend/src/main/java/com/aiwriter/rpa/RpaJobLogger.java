package com.aiwriter.rpa;

@FunctionalInterface
public interface RpaJobLogger {
    void log(String level, String message);

    default void info(String message) {
        log("INFO", message);
    }

    default void success(String message) {
        log("SUCCESS", message);
    }

    default void warn(String message) {
        log("WARN", message);
    }

    default void error(String message) {
        log("ERROR", message);
    }
}
