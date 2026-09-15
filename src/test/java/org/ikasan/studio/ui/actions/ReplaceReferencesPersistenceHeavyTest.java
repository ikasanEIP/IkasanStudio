package org.ikasan.studio.ui.actions;

import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.analysis.ModelReferenceReplacement;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.UiContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Real model persistence and IntelliJ Undo; source generation is tested independently. */
public class ReplaceReferencesPersistenceHeavyTest extends HeavyPlatformTestCase {
    static {
        try { TestFixtures.getBroker(TestFixtures.BASE_META_PACK); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    public void testApplyUndoAndRedoKeepDiskAndLiveModelTogether() throws Exception {
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
        var consumer = TestFixtures.getSpringJmsConsumer(TestFixtures.BASE_META_PACK);
        consumer.setPropertyValue("trustedObjectPackages", "org.example.cat.domain");
        var flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).consumer(consumer).build();
        var module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        var context = myProject.getService(UiContext.class);
        context.setIkasanModule(module);
        context.setPipsiIkasanModel(new GeneratedProjectSynchronizer(myProject) {
            @Override public CompletableFuture<Void> asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        });
        var changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.debug.domain");
        ReplaceReferencesAction.apply(myProject, module, changes);
        VirtualFile projectDirectory = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull("Test project directory must exist", projectDirectory);
        assertTrue(readPersistedModel(projectDirectory).contains("org.example.debug.domain"));
        var undo = UndoManager.getInstance(myProject);
        assertTrue(undo.isUndoAvailable(null));
        var previousDialog = com.intellij.openapi.ui.TestDialogManager.setTestDialog(com.intellij.openapi.ui.TestDialog.OK);
        try {
            undo.undo(null);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("org.example.cat.domain", consumer.getPropertyValue("trustedObjectPackages"));
            assertTrue(readPersistedModel(projectDirectory).contains("org.example.cat.domain"));
            assertTrue(undo.isRedoAvailable(null));
            undo.redo(null);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
            assertTrue(readPersistedModel(projectDirectory).contains("org.example.debug.domain"));
        } finally { com.intellij.openapi.ui.TestDialogManager.setTestDialog(previousDialog); }
    }

    private static String readPersistedModel(VirtualFile projectDirectory) throws IOException {
        projectDirectory.refresh(false, true);
        VirtualFile modelFile = projectDirectory.findFileByRelativePath("generated/src/main/model/model.json");
        assertNotNull("Persisted model must exist", modelFile);
        return VfsUtilCore.loadText(modelFile);
    }
}
