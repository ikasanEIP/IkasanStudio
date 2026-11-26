package org.ikasan.studio.intellij.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.HeavyPlatformTestCase;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.generator.LocalTestEnvironmentTemplate;
import org.ikasan.studio.core.generator.StudioAiSkillTemplates;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.ProtectedModelFileWriter;
import org.ikasan.studio.intellij.project.GenerationTransactionManager.Snapshot;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

    
    public void testNestedApplicationAndDistributionCommitTogetherAndRejectStaleScaffolding() throws Exception {
        VirtualFile workspace = StudioProjectFiles.getWorkspaceBaseDir(myProject);
        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            try {
                VirtualFile jar = workspace.createChildDirectory(this, "jar");
                jar.createChildData(this, ".ikasan-studio-application");
            } catch (IOException failure) { throw new RuntimeException(failure); }
        });
        GenerationTransactionManager.begin(myProject);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/layout-check.txt", "nested", null);
        GenerationTransactionManager.stageWorkspace("bin/launcher.txt", null, "launcher");
        GenerationTransactionManager.commit(myProject);
        assertEquals("nested", new String(workspace.findFileByRelativePath("jar/generated/layout-check.txt").contentsToByteArray(), StandardCharsets.UTF_8));
        assertNotNull(workspace.findFileByRelativePath("bin/launcher.txt"));
        GenerationTransactionManager.begin(myProject);
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/layout-check.txt", "replacement", null);
        GenerationTransactionManager.stageWorkspace("bin/launcher.txt", "stale contents", "replacement");
        try { GenerationTransactionManager.commit(myProject); fail("Expected stale scaffolding rejection"); }
        catch (StudioRuntimeException expected) { assertTrue(expected.getMessage().contains("changed during generation")); }
        assertEquals("nested", new String(workspace.findFileByRelativePath("jar/generated/layout-check.txt").contentsToByteArray(), StandardCharsets.UTF_8));
    }

    public void testScheduledProviderIsCreatedInUserAndSurvivesRegeneration() throws Exception {
        String pack = "V3.3.9";
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        Flow flow = TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        FlowElement consumer = FlowElementFactory.createFlowElement(pack,
                ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer"),
                flow, flow.getFlowRoute(), "Clock");
        consumer.setPropertyValue("messageProvider", "Samples");
        flow.setConsumer(consumer);
        String pkg = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        String userPath = "user/src/main/java/" + pkg.replace('.', '/') + "/Samples.java";
        Method method = GeneratedProjectSynchronizer.class.getDeclaredMethod("generateAndSaveUserImplementClassStubsForFlow",
                Project.class,
                Module.class,
                Flow.class);
        method.setAccessible(true);
        GeneratedProjectSynchronizer synchronizer = new GeneratedProjectSynchronizer(myProject);
        GenerationTransactionManager.begin();
        method.invoke(synchronizer, myProject, module, flow);
        GenerationTransactionManager.commit(myProject);
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull("Test project must have a base directory", base);
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
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull("Test project must have a base directory", base);
        String persistedSource = read(base, path);
        try {
            StudioProjectFiles.checkLegacyPropertyStubLocation(myProject, "example", "Samples");
            fail("Expected relocation guidance");
        } catch (StudioRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Move it"));
        }
        assertEquals(persistedSource, read(base, path));
        assertNull(base.findFileByRelativePath("user/src/main/java/example/Samples.java"));
    }

    public void testStartupGuidanceExistsBeforeAnyGenerationAndPreservesCustomisations() throws Exception {
        assertTrue(ApplicationManager.getApplication().isDispatchThread());
        String basePath = myProject.getBasePath();
        assertNotNull("Test project must have a base path", basePath);
        Path root = Path.of(basePath);
        // The production worker asserts it is not on the EDT. Waiting here does not need EDT work
        // because its VFS refresh is asynchronous, so this also catches accidental invokeAndWait.
        StudioProjectFiles.createStartupGuidanceIfMissing(myProject).get(10, TimeUnit.SECONDS);
        String path = LocalTestEnvironmentTemplate.FILE_NAME;
        assertEquals(LocalTestEnvironmentTemplate.content(),
                Files.readString(root.resolve(path)));
        assertEquals(AiProjectContractGenerator.agentsGuide(),
                Files.readString(root.resolve("AGENTS.md")));
        for (Entry<String,String> entry : StudioAiSkillTemplates.files().entrySet()) {
            assertEquals(entry.getValue(), Files.readString(root.resolve(entry.getKey())));
        }
        Files.writeString(root.resolve(path), "My local settings");
        Files.writeString(root.resolve("AGENTS.md"), "Team instructions");
        CompletableFuture<Void> first = StudioProjectFiles.createStartupGuidanceIfMissing(myProject);
        CompletableFuture<Void> second = StudioProjectFiles.createStartupGuidanceIfMissing(myProject);
        CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);
        assertEquals("My local settings", Files.readString(root.resolve(path)));
        assertEquals("Team instructions", Files.readString(root.resolve("AGENTS.md")));
    }

    public void testLocalEnvironmentTemplateIsCreatedOnceAndPreservesDeveloperEdits() throws Exception {
        String path = LocalTestEnvironmentTemplate.FILE_NAME;
        String template = LocalTestEnvironmentTemplate.content();
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        GenerationTransactionManager.begin();
        StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, template);
        assertNull(base.findFileByRelativePath(path));
        GenerationTransactionManager.commit(myProject);
        assertEquals(template, read(base, path));
        StudioProjectFiles.createFileWithDirectories(myProject, path, "Developer test settings", null);
        VirtualFile file = base.findFileByRelativePath(path);
        assertNotNull(path, file);
        FileDocumentManager documents = FileDocumentManager.getInstance();
        Document document = documents.getDocument(file);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(myProject,
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
        Map<String,String> files = StudioAiSkillTemplates.files();
        GenerationTransactionManager.begin();
        files.forEach((path, content) -> StudioProjectFiles.createFileWithDirectoriesIfMissing(myProject, path, content));
        GenerationTransactionManager.commit(myProject);
        for (Entry<String,String> entry : files.entrySet()) {
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
            if (index == 2) throw new FileSystemException(path, null, "No space left on device");
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
            throw new AccessDeniedException(path);
        }));
        assertTrue(accessDeniedFailure.getMessage().contains("previously existing generated files were restored"));
        assertEquals("original", read(base, "generated/existing.txt"));
    }

    public void testConcurrentModelSavesAndGenerationKeepDeveloperFilesRecoverable() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        StudioProjectFiles.createFileWithDirectories(myProject, "user/owned.txt", "developer", null);
        Path model = Path.of(base.getPath(), "generated", "concurrent-model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, "{\"revision\":0}");
        CountDownLatch started = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            GenerationTransactionManager.begin();
            StudioProjectFiles.createFileWithDirectories(myProject, "generated/concurrent.txt", "generated", null);
            Future<?> saves = pool.submit(() -> {
                started.countDown();
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    for (int i = 1; i <= 20; i++) {
                        ProtectedModelFileWriter.write(model,
                                "{\"revision\":" + i + "}", mapper::readTree);
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            GenerationTransactionManager.commit(myProject);
            saves.get(10, TimeUnit.SECONDS);
            assertEquals("{\"revision\":20}", Files.readString(model));
            assertEquals("{\"revision\":19}", Files.readString(model.resolveSibling("concurrent-model.json.bak.1")));
            assertEquals("generated", read(base, "generated/concurrent.txt"));
            assertEquals("developer", read(base, "user/owned.txt"));
        } finally { pool.shutdownNow(); }
    }

    public void testIndexAccessFailureDuringShorteningKeepsOriginalJavaSource() throws Exception {
        JavaCodeStyleManager style = Mockito.mock(JavaCodeStyleManager.class);
        Mockito.when(style.shortenClassReferences(ArgumentMatchers.any(PsiElement.class)))
                .thenThrow(IndexNotReadyException.create());
        try (MockedStatic<JavaCodeStyleManager> styles = Mockito.mockStatic(JavaCodeStyleManager.class)) {
            styles.when(() -> JavaCodeStyleManager.getInstance(myProject)).thenReturn(style);
            String path = "generated/src/main/java/example/IndexRace.java";
            String source = "package example; public class IndexRace { java.util.List<String> values; }";
            GenerationTransactionManager.begin();
            StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
            assertEquals(1, GenerationTransactionManager.commit(myProject).created());
            VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
            assertNotNull(base);
            assertEquals(source, read(base, path));
            Mockito.verify(style).shortenClassReferences(ArgumentMatchers.any(PsiElement.class));
        }
    }

    public void testJavaGenerationCommitsWhileIndexingWithoutChangingSource() {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        String path = "generated/src/main/java/example/Imported.java";
        String source = "package example; public class Imported { java.util.List<String> values; }";
        StudioProjectFiles.createFileWithDirectories(myProject, "generated/import-state.txt", "before", null);
        DumbModeTestUtils.runInDumbModeSynchronously(myProject, () -> {
            GenerationTransactionManager.begin();
            StudioProjectFiles.createFileWithDirectories(myProject, "generated/import-state.txt", "after", null);
            StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
            GenerationTransactionManager.Summary summary = GenerationTransactionManager.commit(myProject);
            assertEquals(1, summary.created());
            assertEquals(1, summary.updated());
            assertEquals(source, read(base, path));
            assertEquals("after", read(base, "generated/import-state.txt"));
        });
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

        WriteAction.run(() -> {
            try { file.setBinaryContent((formatted + "\n// external generated-file edit\n").getBytes(StandardCharsets.UTF_8)); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
        StudioProjectFiles.createFileWithDirectories(myProject, path, source, null);
        assertFalse(read(base, path).contains("external generated-file edit"));
        StudioProjectFiles.createFileWithDirectories(myProject, path, source.replace("return 1", "return 2"), null);
        assertTrue(read(base, path).contains("return 2"));
    }

    public void testSnapshotRetryPublishesOnlyCompleteAttemptAfterPendingWrite() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        String firstPath = "generated/retry-first.txt";
        String secondPath = "generated/retry-second.txt";
        StudioProjectFiles.createFileWithDirectories(myProject, firstPath, "before write", null);
        StudioProjectFiles.createFileWithDirectories(myProject, secondPath, "second", null);
        VirtualFile first = base.findFileByRelativePath(firstPath);
        VirtualFile second = base.findFileByRelativePath(secondPath);
        assertNotNull(first);
        assertNotNull(second);
        CountDownLatch reachedSecond = new CountDownLatch(1);
        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        AtomicBoolean writeFinished = new AtomicBoolean();
        // Pause after the first rollback entry has been collected. A real pending EDT write
        // cancels the read; its retry must discard that partial list and read the new bytes.
        VirtualFile observedBase = Mockito.spy(base);
        Mockito.doAnswer(invocation -> {
            if (firstAttempt.compareAndSet(true, false)) {
                reachedSecond.countDown();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!writeFinished.get()) {
                    ProgressManager.checkCanceled();
                    if (System.nanoTime() > deadline) throw new AssertionError("Pending write did not cancel the read");
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
            }
            return second;
        }).when(observedBase).findFileByRelativePath(secondPath);
        Future<List<Snapshot>> future = ApplicationManager.getApplication().executeOnPooledThread(() ->
                GenerationTransactionManager.prepareSnapshots(myProject, observedBase, List.of(
                        new GenerationBatch.Artifact(firstPath, "replacement", null),
                        new GenerationBatch.Artifact(secondPath, "replacement", null))));
        try {
            assertTrue("Read must reach the second artifact", reachedSecond.await(10, TimeUnit.SECONDS));
            WriteAction.run(() -> {
                first.setBinaryContent("after write".getBytes(StandardCharsets.UTF_8));
                writeFinished.set(true);
            });
            List<Snapshot> snapshots = future.get(10, TimeUnit.SECONDS);
            assertEquals("An interrupted attempt must not leave extra rollback entries", 2, snapshots.size());
            assertEquals("after write", new String(snapshots.get(0).bytes(), StandardCharsets.UTF_8));
            assertEquals("second", new String(snapshots.get(1).bytes(), StandardCharsets.UTF_8));
            for (Snapshot snapshot : snapshots) snapshot.verifyUnchanged(base);
        } finally {
            writeFinished.set(true);
            future.cancel(true);
        }
    }

    public void testPreparedSnapshotRejectsFileChangedBeforeCommit() throws Exception {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull("Test project must have a base directory", base);
        String path = "generated/snapshot.txt";
        StudioProjectFiles.createFileWithDirectories(myProject, path, "original", null);
        List<Snapshot> snapshots = GenerationTransactionManager.prepareSnapshots(myProject, base,
                List.of(new GenerationBatch.Artifact(path, "replacement", null)));
        assertEquals("original", new String(snapshots.get(0).bytes(), StandardCharsets.UTF_8));
        snapshots.get(0).verifyUnchanged(base);
        VirtualFile file = base.findFileByRelativePath(path);
        assertNotNull(path, file);
        WriteAction.run(() -> {
            try { file.setBinaryContent("external edit".getBytes(StandardCharsets.UTF_8)); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
        assertTrue(expectGenerationFailure(() -> snapshots.get(0).verifyUnchanged(base)).getMessage().contains("File changed"));
        assertEquals("external edit", read(base, path));
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
