package com.aiwriter.service;

import com.aiwriter.model.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService service;

    @BeforeEach
    void setUp() {
        service = new FileService();
        ReflectionTestUtils.setField(service, "dataDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "articlesDir", tempDir.resolve("articles").toString());
    }

    @Test
    void createsSavesReadsAndListsMarkdownFiles() throws Exception {
        String path = service.createFile("draft", "");

        assertThat(path).isEqualTo("draft.md");
        assertThat(service.saveContent(path, "# Hello")).isTrue();
        assertThat(service.readContent(path)).isEqualTo("# Hello");

        List<FileInfo> tree = service.listTree();
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("我的文稿");
        assertThat(tree.get(0).getChildren())
                .extracting(FileInfo::getName)
                .containsExactly("draft.md");
    }

    @Test
    void createsDocumentsInsideFolders() throws Exception {
        String path = service.createFile("nested.md", "ideas");

        assertThat(path).isEqualTo("ideas/nested.md");
        assertThat(Files.exists(tempDir.resolve("articles/ideas/nested.md"))).isTrue();
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> service.readContent("../../etc/passwd"))
                .isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> service.createFile("../escape", ""))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void renameRejectsUnsafeNamesAndAddsMarkdownExtension() throws Exception {
        service.createFile("old.md", "");

        assertThatThrownBy(() -> service.renameFile("old.md", "../escape.md"))
                .isInstanceOf(SecurityException.class);

        String renamed = service.renameFile("old.md", "new-name");

        assertThat(renamed).isEqualTo("new-name.md");
        assertThat(Files.exists(tempDir.resolve("articles/new-name.md"))).isTrue();
    }
}
