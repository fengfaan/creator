package com.aiwriter.ai;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BaoyuSkillsSupport {
    private static final Logger log = LoggerFactory.getLogger(BaoyuSkillsSupport.class);
    private static final String SYSTEM_INSTRUCTIONS = """
            You have access to the following LangChain4j skills:
            %s

            When the user's request relates to one of these skills, call the `activate_skill`
            tool first. Use `read_skill_resource` for referenced files when needed. Do not
            assume scripts can run unless an explicit Java tool provides that capability.
            """;

    private final Skills skills;
    private final String availableSkills;
    private final List<String> loadedSkillNames;

    @Autowired
    public BaoyuSkillsSupport(
            @Value("${app.ai.skills.enabled:true}") boolean enabled,
            @Value("${app.ai.skills.path:}") String configuredPath
    ) {
        this(enabled ? resolveSkillsPath(configuredPath) : null);
    }

    BaoyuSkillsSupport(Path skillsPath) {
        List<FileSystemSkill> loaded = loadBaoyuSkills(skillsPath);
        this.loadedSkillNames = loaded.stream()
                .map(FileSystemSkill::name)
                .toList();
        this.skills = loaded.isEmpty() ? null : Skills.from(loaded);
        this.availableSkills = this.skills == null ? "" : this.skills.formatAvailableSkills();
    }

    static BaoyuSkillsSupport disabled() {
        return new BaoyuSkillsSupport((Path) null);
    }

    boolean available() {
        return skills != null;
    }

    ToolProvider toolProvider() {
        return skills.toolProvider();
    }

    String systemMessage(String existingSystemMessage) {
        String base = existingSystemMessage == null ? "" : existingSystemMessage.trim();
        String skillInstructions = SYSTEM_INSTRUCTIONS.formatted(availableSkills).trim();
        return base.isBlank() ? skillInstructions : base + "\n\n" + skillInstructions;
    }

    List<String> loadedSkillNames() {
        return loadedSkillNames;
    }

    private static Path resolveSkillsPath(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Path.of(configuredPath.trim());
        }
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                userDir.resolve(".agents/skills"),
                userDir.resolve("../.agents/skills").normalize()
        );
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(candidates.getFirst());
    }

    private static List<FileSystemSkill> loadBaoyuSkills(Path skillsPath) {
        if (skillsPath == null || !Files.isDirectory(skillsPath)) {
            return List.of();
        }
        List<FileSystemSkill> loaded = new ArrayList<>();
        try (var directories = Files.list(skillsPath)) {
            directories
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("baoyu-"))
                    .filter(path -> Files.isRegularFile(path.resolve("SKILL.md")))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> loadSkill(path, loaded));
        } catch (Exception e) {
            log.warn("Failed to scan Baoyu skills from {}: {}", skillsPath, e.getMessage());
        }
        if (!loaded.isEmpty()) {
            log.info("Loaded {} Baoyu skills from {}", loaded.size(), skillsPath);
        }
        return loaded;
    }

    private static void loadSkill(Path path, List<FileSystemSkill> loaded) {
        try {
            loaded.add(FileSystemSkillLoader.loadSkill(path));
        } catch (Exception e) {
            log.warn("Skipping Baoyu skill {}: {}", path, e.getMessage());
        }
    }
}
