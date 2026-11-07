package org.ikasan.studio.core.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DeveloperConfigurationImportTest {
    @TempDir Path root;
    private Path write(String path, String text) throws Exception {
        Path target = root.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, text, StandardCharsets.ISO_8859_1);
        return target;
    }
    @Test void developerResourceAndImportWiringPreserveCommentsPlaceholdersAndLineEndings() throws Exception {
        String generated = "# generated defaults\r\nserver.port=8080\r\nspring.config.import=optional:classpath:studio-harness.properties\r\n";
        Path application = write(EnvironmentProperties.APPLICATION_CONFIGURATION, generated);
        String text = "# local café\r\ncustom.endpoint=${SERVICE_URL:http://localhost}\r\ncustom.message=日本\r\n";
        var selection = EnvironmentProperties.Selection.developerOwned(text);
        var preview = EnvironmentProperties.preview(root, selection);
        EnvironmentProperties.apply(root, List.of(preview), false);
        assertThat(Files.readString(root.resolve(EnvironmentProperties.USER_CONFIGURATION), StandardCharsets.ISO_8859_1))
                .isEqualTo(selection.text()).contains("# local café\r\n", "\\u65e5\\u672c", "${SERVICE_URL:http://localhost}");
        assertThat(Files.readString(application)).isEqualTo(generated.replace(
                "optional:classpath:studio-harness.properties", "optional:classpath:studio-harness.properties," + EnvironmentProperties.USER_IMPORT));
        assertThat(root.resolve(".gitignore")).doesNotExist();
        assertThat(root.resolve("config")).doesNotExist();
        assertThat(EnvironmentProperties.withDeveloperImport(Files.readString(application))).isEqualTo(Files.readString(application));
    }
    @Test void refusesChangedDestinationOrGeneratedWiringBeforeWriting() throws Exception {
        Path app = write(EnvironmentProperties.APPLICATION_CONFIGURATION, "server.port=8080\n");
        var preview = EnvironmentProperties.preview(root, EnvironmentProperties.Selection.developerOwned("custom.value=original\n"));
        Files.writeString(app, "server.port=9090\n");
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(preview), true)).hasMessageContaining("changed since comparison");
        assertThat(root.resolve(EnvironmentProperties.USER_CONFIGURATION)).doesNotExist();
        assertThat(Files.readString(app)).isEqualTo("server.port=9090\n");
        var reviewed = EnvironmentProperties.preview(root, EnvironmentProperties.Selection.developerOwned("custom.value=original\n"));
        write(EnvironmentProperties.USER_CONFIGURATION, "custom.value=user-edit\n");
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(reviewed), true)).hasMessageContaining("changed since preview");
        assertThat(Files.readString(root.resolve(EnvironmentProperties.USER_CONFIGURATION))).contains("user-edit");
    }
    @Test void importedResourcesDoNotCreateGeneratedFilesBeforeGeneration() throws Exception {
        var preview = EnvironmentProperties.preview(root, EnvironmentProperties.Selection.developerOwned("custom.value=ready\n"));
        EnvironmentProperties.apply(root, List.of(preview), false);
        assertThat(root.resolve(EnvironmentProperties.USER_CONFIGURATION)).exists();
        assertThat(root.resolve("generated")).doesNotExist();
    }
    @Test void comparisonCanRepairAnInvalidDeveloperFileWithoutParsingItsOriginalContents() throws Exception {
        Path target = write(EnvironmentProperties.USER_CONFIGURATION, "duplicate=one\nduplicate=two\n");
        var snapshot = EnvironmentProperties.previewDeveloperDestination(root);
        assertThat(snapshot.previous()).isEqualTo(Files.readString(target));
        var selected = new EnvironmentProperties.Preview(EnvironmentProperties.Selection.developerOwned("duplicate=chosen\n"),
                snapshot.previous(), List.of(), java.util.Set.of(), java.util.Set.of(), snapshot.applicationPrevious());
        EnvironmentProperties.apply(root, List.of(selected), true);
        assertThat(Files.readString(target)).isEqualTo("duplicate=chosen\n");
        assertThat(EnvironmentProperties.withDeveloperImport("spring.config.import=classpath:studio-local.properties\n"))
                .isEqualTo("spring.config.import=classpath:studio-local.properties\n");
    }

    @Test void rejectsSymlinkAndUnsupportedImportLayoutBeforeWriting() throws Exception {
        write(EnvironmentProperties.APPLICATION_CONFIGURATION, "spring.config.import=optional:classpath:first.properties,\\\n optional:classpath:second.properties\n");
        var preview = EnvironmentProperties.preview(root, EnvironmentProperties.Selection.developerOwned("custom.value=ready\n"));
        assertThatThrownBy(() -> EnvironmentProperties.apply(root, List.of(preview), false)).hasMessageContaining("Regenerate application.properties");
        assertThat(root.resolve(EnvironmentProperties.USER_CONFIGURATION)).doesNotExist();
        Path outside = Files.createTempDirectory(root, "outside");
        Files.createSymbolicLink(root.resolve("user"), outside);
        assertThatThrownBy(() -> EnvironmentProperties.preview(root, EnvironmentProperties.Selection.developerOwned("x=1")))
                .hasMessageContaining("symbolic links");
    }
}
