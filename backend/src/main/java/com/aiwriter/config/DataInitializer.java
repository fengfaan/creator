package com.aiwriter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    @Value("${app.data-dir}")
    private String dataDir;

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void init() throws Exception {
        Path root = Path.of(dataDir);
        Files.createDirectories(root.resolve("articles"));
        Files.createDirectories(root.resolve("assets"));

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """);
        log.info("Data directory initialized at {}", root.toAbsolutePath());
    }
}
