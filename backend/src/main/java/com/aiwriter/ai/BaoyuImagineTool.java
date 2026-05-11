package com.aiwriter.ai;

import com.aiwriter.model.ConfigItem;
import com.aiwriter.service.ConfigService;
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
public class BaoyuImagineTool {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(4);
    private static final List<String> SUPPORTED_RATIOS = List.of("1:1", "16:9", "9:16", "4:3", "3:4", "2.35:1");
    private static final Path DEFAULT_RELATIVE_SCRIPT = Path.of(".agents/skills/baoyu-imagine/scripts/main.ts");
    private static final Path CODEX_HOME_SCRIPT = Path.of(".codex/skills/baoyu-imagine/scripts/main.ts");

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final Path scriptPath;
    private final Path articlesDir;
    private final CommandRunner commandRunner;
    private final Duration timeout;

    @Autowired
    public BaoyuImagineTool(
            ConfigService configService,
            ObjectMapper objectMapper,
            @Value("${app.ai.baoyu-imagine.script:../.agents/skills/baoyu-imagine/scripts/main.ts}") String scriptPath,
            @Value("${app.articles-dir:${app.data-dir}/articles}") String articlesDir
    ) {
        this(
                configService,
                objectMapper,
                resolveScriptPath(scriptPath),
                resolvePath(articlesDir),
                BaoyuImagineTool::runProcess,
                DEFAULT_TIMEOUT
        );
    }

    BaoyuImagineTool(
            ConfigService configService,
            ObjectMapper objectMapper,
            Path scriptPath,
            Path articlesDir,
            CommandRunner commandRunner,
            Duration timeout
    ) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.scriptPath = scriptPath.toAbsolutePath().normalize();
        this.articlesDir = articlesDir.toAbsolutePath().normalize();
        this.commandRunner = commandRunner;
        this.timeout = timeout;
    }

    @Tool(name = "generate_image_with_baoyu_imagine", value = """
            Generate one image using the local baoyu-imagine skill runner. Use this after activating
            baoyu-imagine when the user asks to create or draw an image. The tool returns JSON with
            the saved asset path, public URL, provider, model, and prompt.
            """)
    public String generateImage(
            @P("Detailed image prompt. Prefer English for image models. Do not include secrets.") String prompt,
            @P(value = "Aspect ratio: 1:1, 16:9, 9:16, 4:3, 3:4, or 2.35:1.", required = false) String aspectRatio,
            @P(value = "Quality preset: normal or 2k.", required = false) String quality,
            @P(value = "Provider, for example openai, google, dashscope, openrouter, zai, minimax, replicate, seedream, azure.", required = false) String provider,
            @P(value = "Provider model id. Leave blank to use project config or baoyu defaults.", required = false) String model,
            @P(value = "Short purpose used for file naming, for example cover, inline, xhs.", required = false) String purpose
    ) {
        String safePrompt = requirePrompt(prompt);
        String safeRatio = normalizeAspectRatio(aspectRatio);
        String safeQuality = normalizeQuality(quality);
        String safeProvider = safeOption(provider);
        String safeModel = safeOption(model);
        Path outputPath = nextImagePath(purpose);
        ensureScriptExists();

        List<String> command = new ArrayList<>(List.of(
                "npx", "-y", "bun", scriptPath.toString(),
                "--prompt", safePrompt,
                "--image", outputPath.toString(),
                "--ar", safeRatio,
                "--quality", safeQuality,
                "--json"
        ));
        if (!safeProvider.isBlank()) {
            command.add("--provider");
            command.add(safeProvider);
        }
        if (!safeModel.isBlank()) {
            command.add("--model");
            command.add(safeModel);
        }

        try {
            CommandResult result = commandRunner.run(command, environmentForBaoyu(), scriptPath.getParent(), timeout);
            if (result.exitCode() != 0) {
                return errorJson("baoyu-imagine failed: " + concise(result.stderr()), result);
            }
            JsonNode root = objectMapper.readTree(result.stdout());
            String savedImage = root.path("savedImage").asText(outputPath.toString());
            Path resolvedImage = resolveSavedImage(savedImage, outputPath);
            return objectMapper.createObjectNode()
                    .put("success", true)
                    .put("assetPath", assetPath(resolvedImage))
                    .put("publicUrl", publicUrl(resolvedImage))
                    .put("absolutePath", resolvedImage.toAbsolutePath().normalize().toString())
                    .put("provider", root.path("provider").asText(safeProvider))
                    .put("model", root.path("model").asText(safeModel))
                    .put("prompt", safePrompt)
                    .toPrettyString();
        } catch (Exception e) {
            return errorJson("baoyu-imagine tool error: " + e.getMessage(), null);
        }
    }

    private Map<String, String> environmentForBaoyu() {
        Map<String, String> env = new java.util.HashMap<>();
        putIfPresent(env, "OPENAI_API_KEY", firstConfig("image_api_key", "ai_api_key"));
        putIfPresent(env, "OPENAI_BASE_URL", firstConfig("image_base_url"));
        putIfPresent(env, "OPENAI_IMAGE_MODEL", firstConfig("image_model"));
        putIfPresent(env, "GOOGLE_API_KEY", firstConfig("google_api_key"));
        putIfPresent(env, "DASHSCOPE_API_KEY", firstConfig("dashscope_api_key"));
        putIfPresent(env, "OPENROUTER_API_KEY", firstConfig("openrouter_api_key"));
        putIfPresent(env, "ZAI_API_KEY", firstConfig("zai_api_key", "bigmodel_api_key", "image_api_key"));
        putIfPresent(env, "MINIMAX_API_KEY", firstConfig("minimax_api_key"));
        putIfPresent(env, "REPLICATE_API_TOKEN", firstConfig("replicate_api_token"));
        putIfPresent(env, "ARK_API_KEY", firstConfig("ark_api_key"));
        return env;
    }

    private String firstConfig(String... keys) {
        for (String key : keys) {
            ConfigItem item = configService.get(key);
            if (item != null && item.getValue() != null && !item.getValue().isBlank()) {
                return item.getValue().trim();
            }
        }
        return "";
    }

    private void ensureScriptExists() {
        if (!Files.isRegularFile(scriptPath)) {
            throw new IllegalStateException("baoyu-imagine script not found: " + scriptPath);
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
            return new CommandResult(124, "", "baoyu-imagine timed out");
        }
        return new CommandResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes()),
                new String(process.getErrorStream().readAllBytes())
        );
    }

    private static Path resolveScriptPath(String value) {
        Path configured = resolvePath(value);
        if (Files.isRegularFile(configured)) {
            return configured;
        }
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve(DEFAULT_RELATIVE_SCRIPT),
                cwd.resolve("..").resolve(DEFAULT_RELATIVE_SCRIPT),
                cwd.resolve("../..").resolve(DEFAULT_RELATIVE_SCRIPT),
                Path.of(System.getProperty("user.home", "")).resolve(CODEX_HOME_SCRIPT)
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        return configured;
    }

    private static Path resolvePath(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }

    private Path resolveSavedImage(String savedImage, Path outputPath) {
        Path path = Path.of(savedImage);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path resolved = articlesDir.resolve(path).toAbsolutePath().normalize();
        if (Files.exists(resolved)) {
            return resolved;
        }
        return outputPath.toAbsolutePath().normalize();
    }

    private static void putIfPresent(Map<String, String> env, String key, String value) {
        if (value != null && !value.isBlank()) {
            env.put(key, value);
        }
    }

    private static String requirePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }
        return prompt.trim();
    }

    private static String normalizeAspectRatio(String aspectRatio) {
        String value = aspectRatio == null || aspectRatio.isBlank() ? "1:1" : aspectRatio.trim();
        if (!SUPPORTED_RATIOS.contains(value)) {
            throw new IllegalArgumentException("Unsupported aspect ratio: " + value);
        }
        return value;
    }

    private static String normalizeQuality(String quality) {
        String value = quality == null || quality.isBlank() ? "normal" : quality.trim().toLowerCase(Locale.ROOT);
        if (!value.equals("normal") && !value.equals("2k")) {
            throw new IllegalArgumentException("Unsupported quality: " + value);
        }
        return value;
    }

    private static String safeOption(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeSegment(String value) {
        String normalized = value == null ? "baoyu" : value.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "baoyu" : normalized;
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
}
