package org.ikasan.studio.core.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class MavenOfflineFailureInjectionTest {
    @TempDir Path directory;

    @Test
    void offlineResolutionFailureLeavesModelAndDeveloperSourcesRecoverable() throws Exception {
        Path model = directory.resolve("model.json");
        Path source = directory.resolve("user/Owned.java");
        Files.createDirectories(source.getParent());
        Files.writeString(model, "{\"revision\":1}");
        Files.writeString(source, "// developer implementation");
        Path pom = directory.resolve("pom.xml");
        Files.writeString(pom, """
                <project><modelVersion>4.0.0</modelVersion><groupId>failure.injection</groupId>
                <artifactId>offline</artifactId><version>1</version><dependencies><dependency>
                <groupId>failure.injection.unavailable</groupId><artifactId>missing</artifactId>
                <version>1</version></dependency></dependencies></project>
                """);
        Path log = directory.resolve("maven.log");
        Process process;
        try {
            process = new ProcessBuilder("mvn", "-o", "-B", "-Dmaven.repo.local=" + directory.resolve("empty-repository"),
                    "-f", pom.toString(), "compile").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        } catch (java.io.IOException absentMaven) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Maven executable required: " + absentMaven);
            return;
        }
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Offline Maven did not terminate");
            assertNotEquals(0, process.exitValue());
            assertTrue(Files.readString(log).toLowerCase(java.util.Locale.ROOT).contains("offline"));
            assertEquals("{\"revision\":1}", Files.readString(model));
            assertEquals("// developer implementation", Files.readString(source));
        } finally { process.destroyForcibly(); }
    }
}
