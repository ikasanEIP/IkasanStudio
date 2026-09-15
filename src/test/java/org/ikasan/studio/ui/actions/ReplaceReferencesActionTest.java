package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.command.undo.UnexpectedUndoException;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.analysis.ModelReferenceReplacement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplaceReferencesActionTest {
    @Test void savesAndGeneratesAndRegistersOneGlobalUndoWithReloadProtection() throws Exception { exercise(false); }
    @Test void failedSaveRestoresLiveValuesAndDoesNotRegisterUndo() throws Exception { exercise(true); }

    @Test void opensFromModuleBackgroundWithoutASelectionAndPreservesExplicitSelection() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class);
        var flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK)
                .consumer(TestFixtures.getSpringJmsConsumer(TestFixtures.BASE_META_PACK)).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        when(context.getPropertiesPanel()).thenReturn(panel);
        when(panel.confirmSelectionChangeWithPendingEdits(null)).thenReturn(true);
        try (var dialogs = mockConstruction(ReplaceReferencesDialog.class);
             var files = mockStatic(StudioProjectFiles.class)) {
            ReplaceReferencesAction.open(project);
            verify(panel).updateTargetComponent(module);
            assertEquals(1, dialogs.constructed().size());
            verify(dialogs.constructed().get(0)).show();

            when(context.getSelectedComponent()).thenReturn(flow.getConsumer());
            ReplaceReferencesAction.open(project);
            verify(panel).updateTargetComponent(flow.getConsumer());
            assertEquals(2, dialogs.constructed().size());
            verify(panel, never()).updateTargetComponent(isNull());

            when(panel.confirmSelectionChangeWithPendingEdits(null)).thenReturn(false);
            ReplaceReferencesAction.open(project);
            assertEquals(2, dialogs.constructed().size());
            files.verify(() -> StudioProjectFiles.causeRedraw(project), times(2));
        }
    }

    private void exercise(boolean failSave) throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        var consumer = TestFixtures.getSpringJmsConsumer(TestFixtures.BASE_META_PACK);
        consumer.setPropertyValue("trustedObjectPackages", "org.example.cat.domain");
        var flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).consumer(consumer).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class);
        when(context.getPropertiesPanel()).thenReturn(panel);
        var changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.debug.domain");
        try (var commands = mockStatic(CommandProcessor.class);
             var undos = mockStatic(UndoManager.class);
             var files = mockStatic(StudioProjectFiles.class);
             var applications = mockStatic(ApplicationManager.class)) {
            CommandProcessor command = mock(CommandProcessor.class);
            commands.when(CommandProcessor::getInstance).thenReturn(command);
            doAnswer(call -> { call.getArgument(1, Runnable.class).run(); return null; })
                    .when(command).executeCommand(eq(project), any(Runnable.class), anyString(), isNull());
            UndoManager undo = mock(UndoManager.class);
            undos.when(() -> UndoManager.getInstance(project)).thenReturn(undo);
            Application application = mock(Application.class);
            // Mockito records this invocation to configure the static mock.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
                    .when(application).invokeLater(any(Runnable.class));
            if (failSave) {
                files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)))
                        .thenThrow(new IllegalStateException("Save failed"));
                assertThrows(IllegalStateException.class, () -> ReplaceReferencesAction.apply(project, module, changes));
                assertEquals("org.example.cat.domain", consumer.getPropertyValue("trustedObjectPackages"));
                verifyNoInteractions(undo);
                return;
            }
            ReplaceReferencesAction.apply(project, module, changes);
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
            var captured = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undo).undoableActionPerformed(captured.capture());
            assertTrue(captured.getValue().isGlobal());
            captured.getValue().undo();
            assertEquals("org.example.cat.domain", consumer.getPropertyValue("trustedObjectPackages"));
            captured.getValue().redo();
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)), times(3));
            verify(panel, times(3)).updateTargetComponent(module);
            verify(panel, never()).updateTargetComponent(isNull());
            when(context.getIkasanModule()).thenReturn(null);
            assertThrows(UnexpectedUndoException.class, () -> captured.getValue().undo());
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
        }
    }
}
