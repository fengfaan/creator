package com.aiwriter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@Component
public class DataInitializer {
    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    @Value("${app.data-dir}")
    private String dataDir;

    @Value("${app.articles-dir:${app.data-dir}/articles}")
    private String articlesDir;

    private final JdbcTemplate jdbc;

    public DataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() throws Exception {
        Path root = Path.of(dataDir);
        Path articles = Path.of(articlesDir);
        Files.createDirectories(articles);
        Files.createDirectories(root.resolve("assets"));

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """);
        LOGGER.info("Data directory initialized at " + root.toAbsolutePath());
        LOGGER.info("Articles directory initialized at " + articles.toAbsolutePath());
    }
}
