package com.aiwriter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class BaoyuGeminiWebTool {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;
    private final Path scriptPath;
    private final Path articlesDir;
    private final Path dataDir;
    private final String chromePath;
    private final String chromeProfileDir;
    private final boolean consentAccepted;
    private final CommandRunner commandRunner;
    private final Duration timeout;

    @Autowired
    public BaoyuGeminiWebTool(
            ObjectMapper objectMapper,
            @Value("${app.ai.gemini-web.script:../.agents/skills/baoyu-danger-gemini-web/scripts/main.ts}") String scriptPath,
            @Value("${app.articles-dir:${app.data-dir}/articles}") String articlesDir,
            @Value("${app.ai.gemini-web.data-dir:${app.data-dir}/gemini-web}") String dataDir,
            @Value("${app.ai.gemini-web.chrome-path:}") String chromePath,
            @Value("${app.ai.gemini-web.chrome-profile-dir:}") String chromeProfileDir,
            @Value("${app.ai.gemini-web.consent-accepted:false}") boolean consentAccepted
    ) {
        this(
                objectMapper,
                resolvePath(scriptPath),
                resolvePath(articlesDir),
                resolvePath(dataDir),
                chromePath,
                chromeProfileDir,
                consentAccepted,
                BaoyuGeminiWebTool::runProcess,
                DEFAULT_TIMEOUT
        );
    }

    BaoyuGeminiWebTool(
            ObjectMapper objectMapper,
            Path scriptPath,
            Path articlesDir,
            Path dataDir,
            String chromePath,
            String chromeProfileDir,
            boolean consentAccepted,
            CommandRunner commandRunner,
            Duration timeout
    ) {
        this.objectMapper = objectMapper;
        this.scriptPath = scriptPath.toAbsolutePath().normalize();
        this.articlesDir = articlesDir.toAbsolutePath().normalize();
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.chromePath = chromePath == null ? "" : chromePath.trim();
        this.chromeProfileDir = chromeProfileDir == null ? "" : chromeProfileDir.trim();
        this.consentAccepted = consentAccepted;
        this.commandRunner = commandRunner;
        this.timeout = timeout;
    }

    @Tool(name = "gemini_web_text_with_baoyu", value = """
            Generate text through the local baoyu-danger-gemini-web runner. Use after activating
            baoyu-danger-gemini-web. Requires explicit reverse-engineered Gemini Web consent and
            an authenticated Gemini Web cookie/profile.
            """)
    public String generateText(
            @P("Prompt to send to Gemini Web.") String prompt,
            @P(value = "Model: gemini-3-pro, gemini-3-flash, gemini-3-flash-thinking, or gemini-3.1-pro-preview.", required = false) String model,
            @P(value = "Optional multi-turn session id.", required = false) String sessionId
    ) {
        try {
            ConsentState consent = ensureConsent();
            if (!consent.accepted()) {
                return errorJson(consent.message(), null);
            }
            List<String> command = baseCommand(prompt, model, sessionId);
            CommandResult result = commandRunner.run(command, environmentForGeminiWeb(), scriptPath.getParent(), timeout);
            if (result.exitCode() != 0) {
                return errorJson("baoyu-danger-gemini-web failed: " + concise(result.stderr()), result);
            }
            JsonNode root = objectMapper.readTree(result.stdout());
            return objectMapper.createObjectNode()
                    .put("success", true)
                    .put("text", root.path("text").asText(""))
                    .put("thoughts", root.path("thoughts").asText(""))
                    .put("sessionId", root.path("sessionId").asText(safeOption(sessionId)))
                    .put("model", root.path("model").asText(safeOption(model)))
                    .toPrettyString();
        } catch (Exception e) {
            return errorJson("gemini web text tool error: " + e.getMessage(), null);
        }
    }

    @Tool(name = "gemini_web_image_with_baoyu", value = """
            Generate one image through the local baoyu-danger-gemini-web runner. Use after activating
            baoyu-danger-gemini-web when Gemini Web image generation is requested.
            """)
    public String generateImage(
            @P("Image prompt to send to Gemini Web.") String prompt,
            @P(value = "Model: gemini-3-pro, gemini-3-flash, gemini-3-flash-thinking, or gemini-3.1-pro-preview.", required = false) String model,
            @P(value = "Optional multi-turn session id.", required = false) String sessionId,
            @P(value = "Short purpose used for file naming, for example cover, inline, xhs.", required = false) String purpose
    ) {
        try {
            ConsentState consent = ensureConsent();
            if (!consent.accepted()) {
                return errorJson(consent.message(), null);
            }
            Path outputPath = nextImagePath(purpose);
            List<String> command = baseCommand(prompt, model, sessionId);
            command.add("--image");
            command.add(outputPath.toString());
            CommandResult result = commandRunner.run(command, environmentForGeminiWeb(), scriptPath.getParent(), timeout);
            if (result.exitCode() != 0) {
                return errorJson("baoyu-danger-gemini-web failed: " + concise(result.stderr()), result);
            }
            JsonNode root = objectMapper.readTree(result.stdout());
            Path savedImage = Path.of(root.path("savedImage").asText(outputPath.toString()));
            return objectMapper.createObjectNode()
                    .put("success", true)
                    .put("assetPath", assetPath(savedImage))
                    .put("publicUrl", publicUrl(savedImage))
                    .put("absolutePath", savedImage.toAbsolutePath().normalize().toString())
                    .put("text", root.path("text").asText(""))
                    .put("sessionId", root.path("sessionId").asText(safeOption(sessionId)))
                    .put("model", root.path("model").asText(safeOption(model)))
                    .toPrettyString();
        } catch (Exception e) {
            return errorJson("gemini web image tool error: " + e.getMessage(), null);
        }
    }

    private List<String> baseCommand(String prompt, String model, String sessionId) {
        String safePrompt = requirePrompt(prompt);
        ensureScriptExists();
        List<String> command = new ArrayList<>(List.of(
                "npx", "-y", "bun", scriptPath.toString(),
                "--prompt", safePrompt,
                "--json"
        ));
        String safeModel = safeOption(model);
        if (!safeModel.isBlank()) {
            command.add("--model");
            command.add(safeModel);
        }
        String safeSessionId = safeOption(sessionId);
        if (!safeSessionId.isBlank()) {
            command.add("--sessionId");
            command.add(safeSessionId);
        }
        return command;
    }

    private ConsentState ensureConsent() throws IOException {
        if (!Files.isRegularFile(scriptPath)) {
            throw new IllegalStateException("baoyu-danger-gemini-web script not found: " + scriptPath);
        }
        Path consentFile = dataDir.resolve("consent.json").normalize();
        if (!consentFile.startsWith(dataDir)) {
            throw new SecurityException("Invalid Gemini Web data directory");
        }
        if (Files.isRegularFile(consentFile)) {
            JsonNode root = objectMapper.readTree(consentFile.toFile());
            boolean accepted = root.path("accepted").asBoolean(false)
                    && "1.0".equals(root.path("disclaimerVersion").asText(""));
            if (accepted) {
                return new ConsentState(true, "Consent already accepted");
            }
        }
        if (!consentAccepted) {
            return new ConsentState(false, """
                    Gemini Web requires explicit consent because baoyu-danger-gemini-web uses a reverse-engineered Web API.
                    Set app.ai.gemini-web.consent-accepted=true only after the user accepts that risk.
                    """);
        }
        Files.createDirectories(dataDir);
        Files.writeString(consentFile, """
                {"version":1,"accepted":true,"acceptedAt":"%s","disclaimerVersion":"1.0"}
                """.formatted(java.time.Instant.now()));
        return new ConsentState(true, "Consent accepted by app configuration");
    }

    private Map<String, String> environmentForGeminiWeb() {
        Map<String, String> env = new java.util.HashMap<>();
        env.put("GEMINI_WEB_DATA_DIR", dataDir.toString());
        env.put("GEMINI_WEB_COOKIE_PATH", dataDir.resolve("cookies.json").toString());
        if (!chromePath.isBlank()) {
            env.put("GEMINI_WEB_CHROME_PATH", chromePath);
        }
        if (!chromeProfileDir.isBlank()) {
            env.put("GEMINI_WEB_CHROME_PROFILE_DIR", chromeProfileDir);
        }
        return env;
    }

    private void ensureScriptExists() {
        if (!Files.isRegularFile(scriptPath)) {
            throw new IllegalStateException("baoyu-danger-gemini-web script not found: " + scriptPath);
        }
    }

    private Path nextImagePath(String purpose) {
        String fileName = "%s-%s-%s.png".formatted(
                safeSegment(purpose),
                LocalDateTime.now().format(FILE_TIME),
                UUID.randomUUID().toString().substring(0, 8)
        );
        Path dir = articlesDir.resolve("assets/images").normalize();
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir)) {
            throw new SecurityException("Path traversal detected");
        }
        return file;
    }

    private String assetPath(Path imagePath) {
        Path root = articlesDir.resolve("assets").normalize();
        Path normalized = imagePath.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            return normalized.toString();
        }
        return "assets/" + root.relativize(normalized).toString().replace('\\', '/');
    }

    private String publicUrl(Path imagePath) {
        String assetPath = assetPath(imagePath);
        if (!assetPath.startsWith("assets/images/")) {
            return "";
        }
        return "/api/v1/assets/images/" + assetPath.substring("assets/images/".length());
    }

    private String errorJson(String message, CommandResult result) {
        var root = objectMapper.createObjectNode()
                .put("success", false)
                .put("error", message);
        if (result != null) {
            root.put("exitCode", result.exitCode());
            root.put("stderr", concise(result.stderr()));
        }
        return root.toPrettyString();
    }

    private static CommandResult runProcess(
            List<String> command,
            Map<String, String> environment,
            Path workingDirectory,
            Duration timeout
    ) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.environment().putAll(environment);
        Process process = builder.start();
        boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CommandResult(124, "", "baoyu-danger-gemini-web timed out");
        }
        return new CommandResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes()),
                new String(process.getErrorStream().readAllBytes())
        );
    }

    private static Path resolvePath(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static String requirePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }
        return prompt.trim();
    }

    private static String safeOption(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeSegment(String value) {
        String normalized = value == null ? "gemini-web" : value.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "gemini-web" : normalized;
    }

    private static String concise(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 1200 ? trimmed : trimmed.substring(0, 1200);
    }

    interface CommandRunner {
        CommandResult run(List<String> command, Map<String, String> environment, Path workingDirectory, Duration timeout)
                throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String stdout, String stderr) {
    }

    private record ConsentState(boolean accepted, String message) {
    }
}
