package com.aiwriter.service;

import com.aiwriter.model.ConfigItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("config.db"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """);
        service = new ConfigService(jdbc);
    }

    @Test
    void insertsAndUpdatesConfigValues() {
        service.set("selected_model", "DeepSeek-V3");
        service.set("selected_model", "Qwen-Max");

        ConfigItem item = service.get("selected_model");

        assertThat(item).isNotNull();
        assertThat(item.getValue()).isEqualTo("Qwen-Max");
        assertThat(item.getUpdatedAt()).isNotBlank();
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void returnsNullForMissingConfig() {
        assertThat(service.get("missing")).isNull();
    }
}
