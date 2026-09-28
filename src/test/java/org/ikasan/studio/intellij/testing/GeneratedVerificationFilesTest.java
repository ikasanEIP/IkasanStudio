package org.ikasan.studio.intellij.testing;

import org.ikasan.studio.core.generator.GeneratedVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedVerificationFilesTest {
    @TempDir Path root;
    @Test void refreshWithoutArchivePreservesUnrelatedTestsAndRemovesOldStudioTests() throws Exception {
        var directory = prepareExisting();
        var bundle = new GeneratedVerification.Bundle("new root", "application", "new application",
                Map.of("java/org/ikasan/studio/verification/NewTest.java", "new test"));
        assertNull(GeneratedVerificationFiles.write(root, "original",
                GeneratedVerificationFiles.snapshot(directory), bundle, false));
        assertEquals("new test", Files.readString(directory.resolve("java/org/ikasan/studio/verification/NewTest.java")));
        assertFalse(Files.exists(directory.resolve("java/org/ikasan/studio/verification/OldTest.java")));
        assertEquals("unrelated", Files.readString(directory.resolve("java/OtherTest.java")));
        assertEquals("new root", Files.readString(root.resolve("pom.xml")));
        assertEquals("new application", Files.readString(root.resolve("generated/pom.xml")));
        assertFalse(Files.exists(root.resolve(".gitignore")));
        assertNoTemporaryOrArchiveFiles();
    }

    @Test void failedUnarchivedRefreshRestoresTestsAndBothPoms() throws Exception {
        var directory = prepareExisting();
        String snapshot = GeneratedVerificationFiles.snapshot(directory);
        // Fail writing the root POM after the test tree and application POM were installed.
        var bundle = new GeneratedVerification.Bundle(null, "application", "new application",
                Map.of("java/org/ikasan/studio/verification/NewTest.java", "new test"));
        assertThrows(NullPointerException.class,
                () -> GeneratedVerificationFiles.write(root, "original", snapshot, bundle, false));
        assertEquals(snapshot, GeneratedVerificationFiles.snapshot(directory));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
        assertEquals("application", Files.readString(root.resolve("generated/pom.xml")));
        assertNoTemporaryOrArchiveFiles();
    }

    @Test void unarchivedRefreshStillRejectsChangesAfterApproval() throws Exception {
        var directory = prepareExisting();
        String snapshot = GeneratedVerificationFiles.snapshot(directory);
        Files.writeString(directory.resolve("java/OtherTest.java"), "new developer edit");
        var bundle = new GeneratedVerification.Bundle("new root", "application", "new application", Map.of());
        assertThrows(java.io.IOException.class,
                () -> GeneratedVerificationFiles.write(root, "original", snapshot, bundle, false));
        assertEquals("new developer edit", Files.readString(directory.resolve("java/OtherTest.java")));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
        assertNoTemporaryOrArchiveFiles();
    }

    private Path prepareExisting() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path directory = root.resolve("generated/src/test");
        Files.createDirectories(directory.resolve("java/org/ikasan/studio/verification"));
        Files.writeString(root.resolve("generated/pom.xml"), "application");
        Files.writeString(directory.resolve("java/org/ikasan/studio/verification/OldTest.java"), "developer correction");
        Files.writeString(directory.resolve("java/OtherTest.java"), "unrelated");
        return directory;
    }

    private void assertNoTemporaryOrArchiveFiles() throws Exception {
        try (var paths = Files.walk(root)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".verification-")
                    || path.getFileName().toString().startsWith("test.bak")));
        }
    }

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
