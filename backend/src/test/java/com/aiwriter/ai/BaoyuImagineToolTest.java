package com.aiwriter.ai;

import com.aiwriter.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaoyuImagineToolTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;

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
        configService = new ConfigService(jdbc);
    }

    @Test
    void runsBaoyuImagineWithConfiguredCredentialsAndReturnsAssetJson() throws Exception {
        configService.set("image_api_key", "test-key");
        configService.set("image_base_url", "https://api.example.com/v1");
        configService.set("image_model", "gpt-image-2");
        Path script = tempDir.resolve("skills/baoyu-imagine/scripts/main.ts");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "// test");
        CapturingRunner runner = new CapturingRunner();
        BaoyuImagineTool tool = new BaoyuImagineTool(
                configService,
                new ObjectMapper(),
                script,
                tempDir.resolve("articles"),
                runner,
                Duration.ofSeconds(1)
        );

        String result = tool.generateImage(
                "A luminous mechanical cat",
                "1:1",
                "normal",
                "openai",
                "gpt-image-2",
                "cover"
        );

        assertThat(result)
                .contains("\"success\" : true")
                .contains("\"assetPath\" : \"assets/images/cover-")
                .contains("\"publicUrl\" : \"/api/v1/assets/images/cover-")
                .contains("\"provider\" : \"openai\"")
                .contains("\"model\" : \"gpt-image-2\"");
        assertThat(runner.command).containsSequence(
                "npx", "-y", "bun", script.toAbsolutePath().normalize().toString(),
                "--prompt", "A luminous mechanical cat"
        );
        assertThat(runner.command).contains("--json", "--provider", "openai", "--model", "gpt-image-2");
        assertThat(runner.environment)
                .containsEntry("OPENAI_API_KEY", "test-key")
                .containsEntry("OPENAI_BASE_URL", "https://api.example.com/v1")
                .containsEntry("OPENAI_IMAGE_MODEL", "gpt-image-2");
    }

    @Test
    void mapsSharedImageApiKeyToZaiApiKey() throws Exception {
        configService.set("image_api_key", "glm-key");
        configService.set("image_model", "glm-image");
        Path script = tempDir.resolve("skills/baoyu-imagine/scripts/main.ts");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "// test");
        CapturingRunner runner = new CapturingRunner();
        BaoyuImagineTool tool = new BaoyuImagineTool(
                configService,
                new ObjectMapper(),
                script,
                tempDir.resolve("articles"),
                runner,
                Duration.ofSeconds(1)
        );

        tool.generateImage(
                "A clean editorial illustration",
                "3:4",
                "2k",
                "zai",
                "glm-image",
                "cover"
        );

        assertThat(runner.command).contains("--provider", "zai", "--model", "glm-image");
        assertThat(runner.environment).containsEntry("ZAI_API_KEY", "glm-key");
    }


    private static class CapturingRunner implements BaoyuImagineTool.CommandRunner {
        List<String> command = new ArrayList<>();
        Map<String, String> environment = Map.of();

        @Override
        public BaoyuImagineTool.CommandResult run(
                List<String> command,
                Map<String, String> environment,
                Path workingDirectory,
                Duration timeout
        ) {
            this.command = List.copyOf(command);
            this.environment = Map.copyOf(environment);
            String outputPath = command.get(command.indexOf("--image") + 1);
            return new BaoyuImagineTool.CommandResult(
                    0,
                    """
                    {
                      "savedImage": "%s",
                      "provider": "openai",
                      "model": "gpt-image-2",
                      "attempts": 1,
                      "prompt": "A luminous mechanical cat"
                    }
                    """.formatted(outputPath),
                    ""
            );
        }
    }
}
