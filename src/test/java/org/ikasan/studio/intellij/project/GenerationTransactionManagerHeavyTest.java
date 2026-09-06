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
