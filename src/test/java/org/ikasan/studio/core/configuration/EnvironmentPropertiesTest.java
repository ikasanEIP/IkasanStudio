package org.ikasan.studio.core.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class EnvironmentPropertiesTest {
    @TempDir Path root;

    private Path write(String relative, String contents) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.ISO_8859_1);
        return path;
    }

    // Explicit CRLF and continuation bytes are part of the preservation contract.
    @SuppressWarnings("TextBlockMigration")
    @Test void preservesCommentsEscapesPlaceholdersAndIndependentEnvironmentFiles() throws Exception {
        String text = "# bespoke café\r\ncustom.endpoint=${SERVICE_URL:http://localhost}\r\n"
                + "custom.message=\\u65e5\\u672c\r\ncustom.list=one,\\\r\n two\r\n";
        var dev = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection(text, "dev", false));
        var integration = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("custom.endpoint=https://int\n", "int", false));
        EnvironmentProperties.apply(root, List.of(dev, integration), false);
        assertThat(Files.readAllBytes(root.resolve("config/application-dev.properties")))
                .isEqualTo(text.getBytes(StandardCharsets.ISO_8859_1));
        assertThat(Files.readString(root.resolve("config/application-int.properties"))).contains("https://int");
        assertThat(EnvironmentProperties.parse(text).getProperty("custom.message")).isEqualTo("日本");
        assertThat(root.resolve("generated")).doesNotExist();
    }

    @Test void previewsChangedRemovedAndOverridingKeysWithoutValues() throws Exception {
        write("config/application-dev.properties", "custom.value=old-secret\nobsolete=true\n");
        write("config/application.properties", "custom.value=shared\n");
        Path generated = write("generated/src/main/resources/application.properties", "custom.value=runtime-secret\n");
        var preview = EnvironmentProperties.preview(root,
                new EnvironmentProperties.Selection("custom.value=new-secret\nnew.key=hello\n", "dev", false));
        assertThat(preview.changes()).containsExactly(
                new EnvironmentProperties.Change("custom.value", "changed"),
                new EnvironmentProperties.Change("new.key", "added"),
                new EnvironmentProperties.Change("obsolete", "removed"));
        assertThat(preview.changes().toString()).doesNotContain("secret");
        assertThat(preview.generatedOverrides()).containsExactly("custom.value");
        assertThat(preview.sharedOverrides()).containsExactly("custom.value");
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(preview), false)).hasMessageContaining("Approve replacement");
        EnvironmentProperties.apply(root, List.of(preview), true);
        assertThat(Files.readString(generated)).isEqualTo("custom.value=runtime-secret\n");
        assertThat(Files.readString(root.resolve("config/application-dev.properties"))).doesNotContain("obsolete");
    }

    @Test void refusesStaleOrDuplicateDestinationsBeforeWritingAnything() throws Exception {
        var dev = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("x=1", "dev", false));
        var integration = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("x=2", "int", false));
        write("config/application-int.properties", "x=changed-by-user");
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(dev, integration), true))
                .hasMessageContaining("changed since preview");
        assertThat(root.resolve("config/application-dev.properties")).doesNotExist();
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(dev, dev), false))
                .hasMessageContaining("Multiple imports");
        assertThat(root.resolve("config/application-dev.properties")).doesNotExist();
    }

    @Test void localFilesAreIgnoredAndDoNotTouchShareableFiles() throws Exception {
        write(".gitignore", "target/\n!config/local/\n");
        write("config/application-dev.properties", "x=shared");
        var preview = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("x=local", "dev", true));
        EnvironmentProperties.apply(root, List.of(preview), false);
        assertThat(Files.readString(root.resolve("config/local/application-dev.properties"))).isEqualTo("x=local");
        assertThat(Files.readString(root.resolve("config/application-dev.properties"))).isEqualTo("x=shared");
        assertThat(Files.readString(root.resolve(".gitignore"))).startsWith("target/\n!config/local/\n")
                .endsWith("/config/local/\n");
    }

    @Test void rejectsSymlinkDestinationsIncludingGitignoreBeforeWriting() throws Exception {
        Path outside = Files.createTempDirectory(root, "outside");
        Files.createSymbolicLink(root.resolve("config"), outside);
        assertThatThrownBy(() -> EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("x=1", "dev", false)))
                .hasMessageContaining("symbolic links");
        Files.delete(root.resolve("config"));
        var preview = EnvironmentProperties.preview(root, new EnvironmentProperties.Selection("x=1", "dev", true));
        Files.createSymbolicLink(root.resolve(".gitignore"), outside.resolve("ignore"));
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(preview), false)).hasMessageContaining("symbolic links");
        assertThat(root.resolve("config/local/application-dev.properties")).doesNotExist();
    }

    @Test void validatesProfilesAndPropertiesRatherThanSilentlyChangingMeaning() throws Exception {
        for (String profile : List.of("../prod", "dev,int", "studio-debug", "a/b", "dev.properties"))
            assertThatThrownBy(() -> new EnvironmentProperties.Selection("x=1", profile, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentProperties.parse("x=1\nx=2")).hasMessageContaining("Duplicate property key");
        assertThatThrownBy(() -> EnvironmentProperties.parse("x=\\uZZZZ")).hasMessageContaining("Invalid properties");
        assertThatThrownBy(() -> EnvironmentProperties.parse("x=1\n#---\nx=2")).hasMessageContaining("multi-document");
        for (String key : List.of("spring.profiles.active", "spring.config.import", "spring.config.location"))
            assertThatThrownBy(() -> EnvironmentProperties.preview(root,
                    new EnvironmentProperties.Selection(key + "=other", "dev", false))).hasMessageContaining("launch-control");
        var pasted = new EnvironmentProperties.Selection("hello=日本", "", false);
        assertThat(pasted.relativePath()).isEqualTo("config/application.properties");
        EnvironmentProperties.apply(root, List.of(EnvironmentProperties.preview(root, pasted)), false);
        var loaded = new java.util.Properties();
        try (var stream = Files.newInputStream(root.resolve(pasted.relativePath()))) { loaded.load(stream); }
        assertThat(loaded.getProperty("hello")).isEqualTo("日本");
    }
}
