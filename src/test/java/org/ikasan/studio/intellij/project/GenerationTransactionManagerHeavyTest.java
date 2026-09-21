package org.ikasan.studio.intellij.project;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.StudioRuntimeException;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotEquals;

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

    
    public void testScheduledProviderIsCreatedInUserAndSurvivesRegeneration() throws Exception {
        String pack = "V3.3.9";
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(pack, new java.util.ArrayList<>());
        var flow = org.ikasan.studio.core.TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        var consumer = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(pack,
                org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer"),
                flow, flow.getFlowRoute(), "Clock");
        consumer.setPropertyValue("messageProvider", "Samples");
        flow.setConsumer(consumer);
        String pkg = org.ikasan.studio.core.generator.GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        String userPath = "user/src/main/java/" + pkg.replace('.', '/') + "/Samples.java";
        var method = GeneratedProjectSynchronizer.class.getDeclaredMethod("generateAndSaveUserImplementClassStubsForFlow",
                com.intellij.openapi.project.Project.class,
                org.ikasan.studio.core.model.ikasan.instance.Module.class,
                org.ikasan.studio.core.model.ikasan.instance.Flow.class);
        method.setAccessible(true);
        var synchronizer = new GeneratedProjectSynchronizer(myProject);
        GenerationTransactionManager.begin();
        method.invoke(synchronizer, myProject, module, flow);
        GenerationTransactionManager.commit(myProject);
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base.findFileByRelativePath(userPath));
        assertNull(base.findFileByRelativePath(userPath.replace("user/", "generated/")));
        assertTrue(read(base, userPath).contains("invoke("));
        String custom = "package " + pkg + "; public class Samples { /* developer implementation */ }";
        StudioProjectFiles.createFileWithDirectories(myProject, userPath, custom, null);
        String persistedCustom = read(base, userPath); // IntelliJ formats Java on the first write.
        GenerationTransactionManager.begin();
        method.invoke(synchronizer, myProject, module, flow);
        GenerationTransactionManager.commit(myProject);
        assertEquals(persistedCustom, read(base, userPath));
    }

    public void testLegacyProviderIsPreservedInsteadOfCreatingADuplicate() throws Exception {
        String path = "generated/src/main/java/example/Samples.java";
        String source = "package example; public class Samples { /* existing implementation */ }";
        StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
        String persistedSource = read(StudioProjectFiles.getProjectBaseDir(myProject), path);
        try {
            StudioProjectFiles.checkLegacyPropertyStubLocation(myProject, "example", "Samples");
            fail("Expected relocation guidance");
        } catch (StudioRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Move it"));
        }
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertEquals(persistedSource, read(base, path));
        assertNull(base.findFileByRelativePath("user/src/main/java/example/Samples.java"));
    }

    public void testStartupGuidanceExistsBeforeAnyGenerationAndPreservesCustomisations() throws Exception {
        assertTrue(com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread());
        java.nio.file.Path root = java.nio.file.Path.of(myProject.getBasePath());
        // The production worker asserts it is not on the EDT. Waiting here does not need EDT work
        // because its VFS refresh is asynchronous, so this also catches accidental invokeAndWait.
        StudioProjectFiles.createStartupGuidanceIfMissing(myProject).get(10, java.util.concurrent.TimeUnit.SECONDS);
        String path = org.ikasan.studio.core.generator.LocalTestEnvironmentTemplate.FILE_NAME;
        assertEquals(org.ikasan.studio.core.generator.LocalTestEnvironmentTemplate.content(),
                java.nio.file.Files.readString(root.resolve(path)));
        assertEquals(org.ikasan.studio.core.generator.AiProjectContractGenerator.agentsGuide(),
                java.nio.file.Files.readString(root.resolve("AGENTS.md")));
        for (var entry : org.ikasan.studio.core.generator.StudioAiSkillTemplates.files().entrySet()) {
            assertEquals(entry.getValue(), java.nio.file.Files.readString(root.resolve(entry.getKey())));
        }
        java.nio.file.Files.writeString(root.resolve(path), "My local settings");
        java.nio.file.Files.writeString(root.resolve("AGENTS.md"), "Team instructions");
        var first = StudioProjectFiles.createStartupGuidanceIfMissing(myProject);
        var second = StudioProjectFiles.createStartupGuidanceIfMissing(myProject);
        java.util.concurrent.CompletableFuture.allOf(first, second).get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals("My local settings", java.nio.file.Files.readString(root.resolve(path)));
        assertEquals("Team instructions", java.nio.file.Files.readString(root.resolve("AGENTS.md")));
    }

    public void testLocalEnvironmentTemplateIsCreatedOnceAndPreservesDeveloperEdits() throws Exception {
        String path = org.ikasan.studio.core.generator.LocalTestEnvironmentTemplate.FILE_NAME;
        String template = org.ikasan.studio.core.generator.LocalTestEnvironmentTemplate.content();
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, template);
        assertNull(base.findFileByRelativePath(path));
        GenerationTransactionManager.commit(myProject);
        assertEquals(template, read(base, path));
        StudioProjectFiles.createFileWithDirectories(myProject, path, "Developer test settings", null);
        var file = base.findFileByRelativePath(path);
        var documents = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance();
        var document = documents.getDocument(file);
        assertNotNull(document);
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(myProject,
                () -> document.setText("Unsaved developer settings"));
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, template);
        GenerationTransactionManager.commit(myProject);
        assertEquals("Unsaved developer settings", document.getText());
        assertEquals("Developer test settings", read(base, path));
        assertTrue(documents.isDocumentUnsaved(document));
    }

    public void testProjectSkillFilesAreCreatedAndCustomisationsSurviveGeneration() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        var files = org.ikasan.studio.core.generator.StudioAiSkillTemplates.files();
        GenerationTransactionManager.begin();
        files.forEach((path, content) -> StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, content));
        GenerationTransactionManager.commit(myProject);
        for (var entry : files.entrySet()) {
            assertEquals(entry.getValue(), read(base, entry.getKey()));
            StudioProjectFiles.createFileWithDirectories(myProject, entry.getKey(), "Team workflow customisation", null);
        }
        GenerationTransactionManager.begin();
        files.forEach((path, content) -> StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, content));
        GenerationTransactionManager.commit(myProject);
        for (String path : files.keySet()) assertEquals("Team workflow customisation", read(base, path));
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
        assertTrue(validationFailure.getMessage().contains("generated/Broken.java (line 1, column "));
        assertTrue(validationFailure.getMessage().contains("No source files in this generation batch were replaced"));
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
        assertNotEquals("Fixture must actually be formatted", source, formatted);
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
