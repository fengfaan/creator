package com.aiwriter.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BaoyuSkillsSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsOnlyBaoyuSkillsFromFilesystem() throws Exception {
        writeSkill("baoyu-imagine", "Generate images");
        writeSkill("other-skill", "Should not load");

        BaoyuSkillsSupport support = new BaoyuSkillsSupport(tempDir);

        assertThat(support.available()).isTrue();
        assertThat(support.loadedSkillNames()).containsExactly("baoyu-imagine");
        assertThat(support.systemMessage("base instructions"))
                .contains("base instructions")
                .contains("<available_skills>")
                .contains("baoyu-imagine")
                .contains("activate_skill");
    }

    @Test
    void isUnavailableWhenDirectoryDoesNotExist() {
        BaoyuSkillsSupport support = new BaoyuSkillsSupport(tempDir.resolve("missing"));

        assertThat(support.available()).isFalse();
        assertThat(support.loadedSkillNames()).isEmpty();
    }

    private void writeSkill(String name, String description) throws Exception {
        Path skillDir = tempDir.resolve(name);
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: %s
                description: %s
                ---

                Follow the skill instructions.
                """.formatted(name, description));
    }
}
