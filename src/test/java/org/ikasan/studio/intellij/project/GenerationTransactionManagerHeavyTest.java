package org.ikasan.studio.intellij.project;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.StudioRuntimeException;

import java.nio.charset.StandardCharsets;

/** Exercises the file-ownership and all-or-nothing guarantees against a real project VFS. */
public class GenerationTransactionManagerHeavyTest extends HeavyPlatformTestCase {
    private static final String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData" + TEST_DATA_DIR);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            GenerationTransactionManager.abort();
        } finally {
            super.tearDown();
        }
    }

    
    public void testGenerationIsValidatedAndCommittedAsOneOwnedBatch() throws Exception {
        VirtualFile baseDir = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(baseDir);

        // Staging has no visible filesystem side effects.
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/staged.txt", "staged", null);
        assertNull(baseDir.findFileByRelativePath("generated/staged.txt"));
        GenerationTransactionManager.Summary first = GenerationTransactionManager.commit(myProject);
        assertEquals(1, first.created());
        assertEquals("staged", read(baseDir, "generated/staged.txt"));

        // A bad artifact prevents every otherwise-valid artifact in the same batch from being written.
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/must-not-appear.txt", "safe", null);
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/Broken.java", "class {", null);
        StudioRuntimeException validationFailure = expectGenerationFailure(
                () -> GenerationTransactionManager.commit(myProject));
        assertTrue(validationFailure.getMessage().contains("Generated Java is invalid"));
        assertNull(baseDir.findFileByRelativePath("generated/must-not-appear.txt"));

        // Existing developer files cannot be changed merely because a generator targeted them.
        StudioProjectFiles.createFileWithDirectories(myProject, "/user/src/main/java/example/Owned.java",
                "package example;\nclass Owned { }\n", null);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "/user/src/main/java/example/Owned.java",
                "package example;\nclass Owned { int changed; }\n", null);
        StudioRuntimeException ownershipFailure = expectGenerationFailure(
                () -> GenerationTransactionManager.commit(myProject));
        assertTrue(ownershipFailure.getMessage().contains("explicit confirmation"));
        assertFalse(read(baseDir, "user/src/main/java/example/Owned.java").contains("changed"));

        // Explicit authorization applies only to the exact developer-owned path.
        GenerationTransactionManager.begin();
        GenerationTransactionManager.authoriseUserReplacement("user/src/main/java/example/Owned.java");
        StudioProjectFiles.createFileWithDirectories(myProject, "/user/src/main/java/example/Owned.java",
                "package example;\nclass Owned { int changed; }\n", null);
        GenerationTransactionManager.Summary replacement = GenerationTransactionManager.commit(myProject);
        assertEquals(1, replacement.updated());
        assertTrue(read(baseDir, "user/src/main/java/example/Owned.java").contains("changed"));

        // Replacing an existing file retains its line-ending convention and UTF-8 content.
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/application.properties",
                "first=old\r\nsecond=café\r\n", null);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/application.properties",
                "first=new\nsecond=café\n", null);
        GenerationTransactionManager.commit(myProject);
        assertEquals("first=new\r\nsecond=café\r\n", read(baseDir, "generated/application.properties"));
    }

    public void testDiskFullMidCommitRollsBackEarlierWritesAndAllowsRetry() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/existing.txt", "original", null);
        StudioProjectFiles.createFileWithDirectories(myProject, "user/owned.txt", "developer", null);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/existing.txt", "replacement", null);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/new.txt", "new", null);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/fails.txt", "cannot fit", null);
        StudioRuntimeException diskFullFailure = expectGenerationFailure(() -> GenerationTransactionManager.commit(myProject, (path, index) -> {
            if (index == 2) throw new java.nio.file.FileSystemException(path, null, "No space left on device");
        }));
        assertTrue(diskFullFailure.getMessage().contains("previously existing generated files were restored"));
        assertEquals("original", read(base, "generated/existing.txt"));
        assertEquals("developer", read(base, "user/owned.txt"));
        assertNull(base.findFileByRelativePath("generated/new.txt"));
        assertNull(base.findFileByRelativePath("generated/fails.txt"));
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/existing.txt", "retry", null);
        GenerationTransactionManager.commit(myProject);
        assertEquals("retry", read(base, "generated/existing.txt"));
    }

    public void testReadOnlyCommitPreservesFiles() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/existing.txt", "original", null);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/existing.txt", "replacement", null);
        StudioRuntimeException accessDeniedFailure = expectGenerationFailure(() -> GenerationTransactionManager.commit(myProject, (path, index) -> {
            throw new java.nio.file.AccessDeniedException(path);
        }));
        assertTrue(accessDeniedFailure.getMessage().contains("previously existing generated files were restored"));
        assertEquals("original", read(base, "generated/existing.txt"));
    }

    public void testConcurrentModelSavesAndGenerationKeepDeveloperFilesRecoverable() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        StudioProjectFiles.createFileWithDirectories(myProject, "user/owned.txt", "developer", null);
        var model = java.nio.file.Path.of(base.getPath(), "generated", "concurrent-model.json");
        java.nio.file.Files.createDirectories(model.getParent());
        java.nio.file.Files.writeString(model, "{\"revision\":0}");
        var started = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            GenerationTransactionManager.begin();
            StudioProjectFiles.createFileWithDirectories(myProject, "generated/concurrent.txt", "generated", null);
            var saves = pool.submit(() -> {
                started.countDown();
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    for (int i = 1; i <= 20; i++) {
                        org.ikasan.studio.core.persistence.json.ProtectedModelFileWriter.write(model,
                                "{\"revision\":" + i + "}", mapper::readTree);
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            GenerationTransactionManager.commit(myProject);
            saves.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals("{\"revision\":20}", java.nio.file.Files.readString(model));
            assertEquals("{\"revision\":19}", java.nio.file.Files.readString(model.resolveSibling("concurrent-model.json.bak.1")));
            assertEquals("generated", read(base, "generated/concurrent.txt"));
            assertEquals("developer", read(base, "user/owned.txt"));
        } finally { pool.shutdownNow(); }
    }

    public void testFormattedGenerationIsReusedOnlyWhileTemplateAndDiskStillMatch() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        String path = "generated/src/main/java/example/Cached.java";
        String source = "package example; public class Cached { public int value(){return 1;} }";
        StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
        VirtualFile file = base.findFileByRelativePath(path);
        assertNotNull(file);
        long initialStamp = file.getModificationStamp();
        String formatted = read(base, path);
        assertFalse("Fixture must actually be formatted", source.equals(formatted));
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
        assertEquals(1, GenerationTransactionManager.commit(myProject).unchanged());
        assertEquals(initialStamp, file.getModificationStamp());
        assertEquals(formatted, read(base, path));

        com.intellij.openapi.application.WriteAction.run(() -> {
            try { file.setBinaryContent((formatted + "\n// external generated-file edit\n").getBytes(StandardCharsets.UTF_8)); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
        assertFalse(read(base, path).contains("external generated-file edit"));
        StudioProjectFiles.createFileWithDirectories(myProject, path, source.replace("return 1", "return 2"), null);
        assertTrue(read(base, path).contains("return 2"));
    }

    private static StudioRuntimeException expectGenerationFailure(Runnable action) {
        try {
            action.run();
            fail("Expected generation to fail");
            throw new AssertionError("unreachable");
        } catch (StudioRuntimeException expected) {
            return expected;
        }
    }

    private static String read(VirtualFile baseDir, String relativePath) throws Exception {
        VirtualFile file = baseDir.findFileByRelativePath(relativePath);
        assertNotNull(relativePath, file);
        return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
    }
}
