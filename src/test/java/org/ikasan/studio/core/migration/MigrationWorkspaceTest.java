package org.ikasan.studio.core.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MigrationWorkspaceTest {
    @TempDir Path root;

    @Test void snapshotRestoresExactBytesAndRemovesNewFilesWhilePreservingUserCode() throws Exception {
        byte[] original = "<project>café</project>\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(root.resolve("pom.xml"), original);
        Files.createDirectories(root.resolve("user"));
        Files.writeString(root.resolve("user/Owned.java"), "keep");
        var changes = MigrationWorkspace.prepare(root, Map.of("pom.xml", "updated", "generated/new.txt", "new"));
        assertThat(Files.exists(root.resolve("generated/new.txt"))).isFalse();
        MigrationWorkspace.commit(root, changes, "test report");
        var snapshot = MigrationWorkspace.latest(root);
        assertThat(snapshot.report()).isEqualTo("test report");
        MigrationWorkspace.commit(root, MigrationWorkspace.prepareRestore(root, snapshot), "restore");
        assertThat(Files.readAllBytes(root.resolve("pom.xml"))).isEqualTo(original);
        assertThat(Files.exists(root.resolve("generated/new.txt"))).isFalse();
        assertThat(Files.readString(root.resolve("user/Owned.java"))).isEqualTo("keep");
    }

    @Test void stalePreviewAbortsBeforeWritingAnything() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var proposed = new LinkedHashMap<String, String>();
        proposed.put("generated/new.txt", "must not appear");
        proposed.put("pom.xml", "target");
        var changes = MigrationWorkspace.prepare(root, proposed);
        Files.writeString(root.resolve("pom.xml"), "external edit");
        assertThatThrownBy(() -> MigrationWorkspace.commit(root, changes, "test")).hasMessageContaining("changed after preview");
        assertThat(Files.exists(root.resolve("generated/new.txt"))).isFalse();
        assertThat(Files.readString(root.resolve("pom.xml"))).isEqualTo("external edit");
    }

    @Test void restorePreservesTeamInstructionsAddedAfterMigration() throws Exception {
        MigrationWorkspace.commit(root, MigrationWorkspace.prepare(root, Map.of("AGENTS.md", "generated guide")), "test");
        var snapshot = MigrationWorkspace.latest(root);
        Files.writeString(root.resolve("AGENTS.md"), "Corporate instructions");
        assertThat(MigrationWorkspace.prepareRestore(root, snapshot)).isEmpty();
        assertThat(Files.readString(root.resolve("AGENTS.md"))).isEqualTo("Corporate instructions");
    }

    @Test void writeFailureRollsBackEarlierChanges() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Files.createDirectories(root.resolve("generated"));
        Files.writeString(root.resolve("generated/not-a-directory"), "keep");
        var artifacts = new LinkedHashMap<String, String>();
        artifacts.put("pom.xml", "target");
        artifacts.put("generated/not-a-directory/child.txt", "cannot write");
        var changes = MigrationWorkspace.prepare(root, artifacts);
        assertThatThrownBy(() -> MigrationWorkspace.commit(root, changes, "test")).hasMessageContaining("restoring original files");
        assertThat(Files.readString(root.resolve("pom.xml"))).isEqualTo("original");
        assertThat(Files.readString(root.resolve("generated/not-a-directory"))).isEqualTo("keep");
    }

    @Test void refusesDeveloperPathsTraversalAndSymlinks() throws Exception {
        for (String path : new String[]{"user/Owned.java", "generated/../../outside", "/tmp/outside"}) {
            assertThatThrownBy(() -> MigrationWorkspace.prepare(root, Map.of(path, "bad"))).isInstanceOf(java.io.IOException.class);
        }
        Files.createSymbolicLink(root.resolve("generated"), root.resolve("user"));
        assertThatThrownBy(() -> MigrationWorkspace.prepare(root, Map.of("generated/Owned.java", "bad"))).hasMessageContaining("symbolic links");
    }
}
