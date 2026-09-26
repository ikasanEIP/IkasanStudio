package org.ikasan.studio.intellij.testing;

import org.ikasan.studio.core.generator.GeneratedVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedVerificationFilesTest {
    @TempDir Path root;
    @Test void refreshArchivesCorrectionsAndRejectsStaleApproval() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var bundle = new GeneratedVerification.Bundle("updated", Map.of("src/test/java/Test.java", "generated", "README.md", "readme"));
        assertNull(GeneratedVerificationFiles.write(root, "original", "missing", bundle));
        Path directory = root.resolve("generated-verification");
        Path test = directory.resolve("src/test/java/Test.java");
        String initial = GeneratedVerificationFiles.snapshot(directory);
        Files.writeString(test, "developer correction");
        assertThrows(java.io.IOException.class, () -> GeneratedVerificationFiles.write(root, "updated", initial, bundle));
        assertEquals("developer correction", Files.readString(test));
        Path backup = GeneratedVerificationFiles.write(root, "updated", GeneratedVerificationFiles.snapshot(directory), bundle);
        assertEquals("developer correction", Files.readString(backup.resolve("src/test/java/Test.java")));
        assertEquals("generated", Files.readString(test));
    }
}
