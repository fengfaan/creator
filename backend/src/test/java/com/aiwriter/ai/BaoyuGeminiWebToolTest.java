package com.aiwriter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaoyuGeminiWebToolTest {

    @TempDir
    Path tempDir;

    @Test
    void refusesToRunWithoutConsent() throws Exception {
        Path script = writeScript();
        CapturingRunner runner = new CapturingRunner("""
                {"text":"unused","model":"gemini-3-flash"}
                """);
        BaoyuGeminiWebTool tool = tool(script, false, runner);

        String result = tool.generateText("你好，Gemini", "gemini-3-flash", "");

        assertThat(result)
                .contains("\"success\" : false")
                .contains("requires explicit consent");
        assertThat(runner.command).isEmpty();
    }

    @Test
    void runsTextGenerationWhenConsentAccepted() throws Exception {
        Path script = writeScript();
        CapturingRunner runner = new CapturingRunner("""
                {
                  "text": "你好，我是 Gemini",
                  "thoughts": "",
                  "sessionId": "s1",
                  "model": "gemini-3-flash"
                }
                """);
        BaoyuGeminiWebTool tool = tool(script, true, runner);

        String result = tool.generateText("你好，Gemini", "gemini-3-flash", "s1");

        assertThat(result)
                .contains("\"success\" : true")
                .contains("你好，我是 Gemini")
                .contains("\"sessionId\" : \"s1\"")
                .contains("\"model\" : \"gemini-3-flash\"");
        assertThat(runner.command).containsSequence(
                "npx", "-y", "bun", script.toAbsolutePath().normalize().toString(),
                "--prompt", "你好，Gemini",
                "--json",
                "--model", "gemini-3-flash",
                "--sessionId", "s1"
        );
        assertThat(runner.environment)
                .containsKey("GEMINI_WEB_DATA_DIR")
                .containsKey("GEMINI_WEB_COOKIE_PATH")
                .containsEntry("GEMINI_WEB_CHROME_PATH", "/Applications/Google Chrome 2.app/Contents/MacOS/Google Chrome")
                .containsEntry("GEMINI_WEB_CHROME_PROFILE_DIR", "/Users/fengfan/Library/Application Support/Google/Chrome");
    }

    @Test
    void runsImageGenerationAndReturnsAssetPath() throws Exception {
        Path script = writeScript();
        CapturingRunner runner = new CapturingRunner(null);
        BaoyuGeminiWebTool tool = tool(script, true, runner);
        runner.stdoutProvider = command -> {
            String outputPath = command.get(command.indexOf("--image") + 1);
            return """
                    {
                      "text": "done",
                      "savedImage": "%s",
                      "sessionId": "img-session",
                      "model": "gemini-3-pro"
                    }
                    """.formatted(outputPath);
        };

        String result = tool.generateImage("A cat", "gemini-3-pro", "img-session", "cover");

        assertThat(result)
                .contains("\"success\" : true")
                .contains("\"assetPath\" : \"assets/images/cover-")
                .contains("\"publicUrl\" : \"/api/v1/assets/images/cover-")
                .contains("\"absolutePath\"");
        assertThat(runner.command).contains("--image");
    }

    private BaoyuGeminiWebTool tool(Path script, boolean consentAccepted, CapturingRunner runner) {
        return new BaoyuGeminiWebTool(
                new ObjectMapper(),
                script,
                tempDir.resolve("articles"),
                tempDir.resolve("gemini-web"),
                "/Applications/Google Chrome 2.app/Contents/MacOS/Google Chrome",
                "/Users/fengfan/Library/Application Support/Google/Chrome",
                consentAccepted,
                runner,
                Duration.ofSeconds(1)
        );
    }

    private Path writeScript() throws Exception {
        Path script = tempDir.resolve("skills/baoyu-danger-gemini-web/scripts/main.ts");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "// test");
        return script;
    }

    private static class CapturingRunner implements BaoyuGeminiWebTool.CommandRunner {
        List<String> command = new ArrayList<>();
        Map<String, String> environment = Map.of();
        OutputProvider stdoutProvider;

        CapturingRunner(String stdout) {
            this.stdoutProvider = command -> stdout;
        }

        @Override
        public BaoyuGeminiWebTool.CommandResult run(
                List<String> command,
                Map<String, String> environment,
                Path workingDirectory,
                Duration timeout
        ) {
            this.command = List.copyOf(command);
            this.environment = Map.copyOf(environment);
            return new BaoyuGeminiWebTool.CommandResult(0, stdoutProvider.stdout(command), "");
        }
    }

    private interface OutputProvider {
        String stdout(List<String> command);
    }
}
