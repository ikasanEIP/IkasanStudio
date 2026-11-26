package org.ikasan.studio.intellij.onboarding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanStudioProjectDetectorTest {
    @TempDir
    Path projectRoot;

    @Test
    void markerIdentifiesAProjectWithoutRequiringTheLegacyLayout() throws IOException {
        Files.createFile(projectRoot.resolve(IkasanStudioProjectDetector.PROJECT_MARKER));

        assertThat(IkasanStudioProjectDetector.isIkasanStudioProject(projectRoot)).isTrue();
    }

    @Test
    void legacyArchetypeLayoutIsIdentified() throws IOException {
        createLegacyProject("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modules>
                    <module>generated</module>
                    <module>user</module>
                  </modules>
                </project>
                """);

        assertThat(IkasanStudioProjectDetector.isIkasanStudioProject(projectRoot)).isTrue();
    }

    @Test
    void ordinaryMultiModuleMavenProjectIsNotIdentified() throws IOException {
        createLegacyProject("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modules>
                    <module>server</module>
                    <module>client</module>
                  </modules>
                </project>
                """);

        assertThat(IkasanStudioProjectDetector.isIkasanStudioProject(projectRoot)).isFalse();
    }

    @Test
    void incompleteLegacyLayoutIsNotIdentified() throws IOException {
        Files.writeString(projectRoot.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modules><module>generated</module><module>user</module></modules>
                </project>
                """);

        assertThat(IkasanStudioProjectDetector.isIkasanStudioProject(projectRoot)).isFalse();
    }

    @Test
    void nestedApplicationReactorIsDetectedWithoutAMarker() throws IOException {
        createLegacyProject("<project><modules><module>generated</module><module>user</module></modules></project>");
        Path jar = Files.createDirectory(projectRoot.resolve("jar"));
        for (String name : new String[]{"generated", "user", "pom.xml"})
            Files.move(projectRoot.resolve(name), jar.resolve(name));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project><modules><module>jar</module><module>distribution</module></modules></project>");
        assertThat(IkasanStudioProjectDetector.isIkasanStudioProject(projectRoot)).isTrue();
    }

    private void createLegacyProject(String rootPom) throws IOException {
        Files.writeString(projectRoot.resolve("pom.xml"), rootPom);
        Files.createDirectories(projectRoot.resolve("generated/src/main/model"));
        Files.createDirectories(projectRoot.resolve("user"));
        Files.writeString(projectRoot.resolve("generated/pom.xml"), "<project/>");
        Files.writeString(projectRoot.resolve("user/pom.xml"), "<project/>");
        Files.writeString(projectRoot.resolve("generated/src/main/model/model.json"), "{}");
    }
}
