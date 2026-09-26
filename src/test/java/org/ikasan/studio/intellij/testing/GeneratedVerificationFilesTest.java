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
        Files.writeString(root.resolve(".gitignore"), "# Existing rules\ntarget/");
        Files.createDirectories(root.resolve("generated"));
        Files.writeString(root.resolve("generated/pom.xml"), "application");
        Files.createDirectories(root.resolve("generated/src/main"));
        Files.writeString(root.resolve("generated/src/main/application.txt"), "application source");
        var bundle = new GeneratedVerification.Bundle("updated", "application", "application", Map.of("java/org/ikasan/studio/verification/Test.java", "generated", "resources/studio-verification/README.md", "readme"));
        assertNull(GeneratedVerificationFiles.write(root, "original", "missing", bundle));
        String ignore = Files.readString(root.resolve(".gitignore"));
        assertTrue(ignore.startsWith("# Existing rules\ntarget/\n"));
        assertTrue(ignore.contains("/generated/src/test.bak*/"));
        Path directory = root.resolve("generated/src/test");
        Path test = directory.resolve("java/org/ikasan/studio/verification/Test.java");
        Files.writeString(directory.resolve("java/OtherTest.java"), "unrelated");
        String initial = GeneratedVerificationFiles.snapshot(directory);
        Files.writeString(test, "developer correction");
        assertThrows(java.io.IOException.class, () -> GeneratedVerificationFiles.write(root, "updated", initial, bundle));
        assertEquals("developer correction", Files.readString(test));
        Path backup = GeneratedVerificationFiles.write(root, "updated", GeneratedVerificationFiles.snapshot(directory), bundle);
        assertEquals("developer correction", Files.readString(backup.resolve("java/org/ikasan/studio/verification/Test.java")));
        assertEquals(ignore, Files.readString(root.resolve(".gitignore")));
        assertEquals("generated", Files.readString(test));
        assertEquals("unrelated", Files.readString(directory.resolve("java/OtherTest.java")));
        assertEquals("application source", Files.readString(root.resolve("generated/src/main/application.txt")));
        String approved = GeneratedVerificationFiles.snapshot(directory);
        Files.writeString(root.resolve("generated/pom.xml"), "external edit");
        assertThrows(java.io.IOException.class, () -> GeneratedVerificationFiles.write(root, "updated", approved, bundle));
        assertEquals("external edit", Files.readString(root.resolve("generated/pom.xml")));
    }
}
