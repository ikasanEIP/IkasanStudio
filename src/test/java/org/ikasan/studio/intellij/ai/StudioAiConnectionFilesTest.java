package org.ikasan.studio.intellij.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class StudioAiConnectionFilesTest {
    @TempDir Path root;

    @Test void stablePathsRotateSessionAndExcludeConcurrentOwners() throws Exception {
        String project = root.resolve("project").toString();
        Path connection;
        Path adapter;
        try (var first = StudioAiConnectionFiles.open(root, project, new byte[]{1, 2})) {
            connection = first.connection();
            adapter = first.adapter();
            first.publish("first-session");
            assertThatThrownBy(() -> StudioAiConnectionFiles.open(root, project, new byte[]{3}))
                    .hasMessageContaining("another IDE session");
            assertThat(Files.readString(connection)).isEqualTo("first-session");
            try (var other = StudioAiConnectionFiles.open(root, root.resolve("other").toString(), new byte[]{4})) {
                assertThat(other.connection()).isNotEqualTo(connection);
            }
            if (Files.getFileStore(connection).supportsFileAttributeView("posix")) {
                assertThat(Files.getPosixFilePermissions(connection)).isEqualTo(
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        }
        assertThat(connection).doesNotExist();
        try (var reopened = StudioAiConnectionFiles.open(root, project, new byte[]{3})) {
            assertThat(reopened.connection()).isEqualTo(connection);
            assertThat(reopened.adapter()).isEqualTo(adapter);
            reopened.publish("new-session");
            assertThat(Files.readString(connection)).isEqualTo("new-session");
            assertThat(Files.readAllBytes(adapter)).containsExactly((byte) 3);
        }
    }
}
