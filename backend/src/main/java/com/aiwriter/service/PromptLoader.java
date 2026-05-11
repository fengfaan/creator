package com.aiwriter.service;

import com.aiwriter.model.ConfigItem;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptLoader {
    private final ConfigService configService;
    private final Map<String, String> classpathCache = new ConcurrentHashMap<>();

    public PromptLoader(ConfigService configService) {
        this.configService = configService;
    }

    public String get(String name) {
        if (configService != null) {
            ConfigItem override = configService.get("prompt:" + name);
            if (override != null && override.getValue() != null && !override.getValue().isBlank()) {
                return override.getValue();
            }
        }
        return loadFromClasspath(name);
    }

    private String loadFromClasspath(String name) {
        return classpathCache.computeIfAbsent(name, key -> {
            try (var is = getClass().getResourceAsStream("/prompts/" + key + ".txt")) {
                if (is == null) {
                    throw new IllegalStateException("Prompt template not found: " + key);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static String format(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
