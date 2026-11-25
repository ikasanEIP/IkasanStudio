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
import org.ikasan.studio.core.model.analysis.ModelReferenceReplacement.Change;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class ReplaceReferencesActionTest {
    @Test void savesAndGeneratesAndRegistersOneGlobalUndoWithReloadProtection() throws Exception { exercise(false); }
    @Test void failedSaveRestoresLiveValuesAndDoesNotRegisterUndo() throws Exception { exercise(true); }

    @Test void opensFromModuleBackgroundWithoutASelectionAndPreservesExplicitSelection() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class);
        Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK)
                .consumer(TestFixtures.getSpringJmsConsumer(TestFixtures.BASE_META_PACK)).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        when(context.getPropertiesPanel()).thenReturn(panel);
        when(panel.confirmSelectionChangeWithPendingEdits(null)).thenReturn(true);
        try (MockedConstruction<ReplaceReferencesDialog> dialogs = mockConstruction(ReplaceReferencesDialog.class);
             MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class)) {
            ReplaceReferencesAction.open(project);
            verify(panel).updateTargetComponent(module);
            assertEquals(1, dialogs.constructed().size());
            verify(dialogs.constructed().get(0)).show();

            when(context.getSelectedComponent()).thenReturn(flow.getConsumer());
            ReplaceReferencesAction.open(project);
            verify(panel).updateTargetComponent(flow.getConsumer());
            assertEquals(2, dialogs.constructed().size());
            verify(dialogs.constructed().get(1)).show();
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
        FlowElement consumer = TestFixtures.getSpringJmsConsumer(TestFixtures.BASE_META_PACK);
        consumer.setPropertyValue("trustedObjectPackages", "org.example.cat.domain");
        Flow flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).consumer(consumer).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class);
        when(context.getPropertiesPanel()).thenReturn(panel);
        List<Change> changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.debug.domain");
        try (MockedStatic<CommandProcessor> commands = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undos = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class);
             MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class)) {
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
            if (failSave) {
                files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)))
                        .thenThrow(new IllegalStateException("Save failed"));
                assertThrows(IllegalStateException.class, () -> ReplaceReferencesAction.apply(project, module, changes));
                assertEquals("org.example.cat.domain", consumer.getPropertyValue("trustedObjectPackages"));
                files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)));
                verifyNoInteractions(undo, command, application);
                return;
            }
            CompletableFuture<Void> generation = new CompletableFuture<>();
            files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)))
                    .thenReturn(generation);
            assertSame(generation, ReplaceReferencesAction.apply(project, module, changes));
            assertFalse(generation.isDone());
            generation.complete(null);
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
            ArgumentCaptor<UndoableAction> captured = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undo).undoableActionPerformed(captured.capture());
            assertTrue(captured.getValue().isGlobal());
            captured.getValue().undo();
            assertEquals("org.example.cat.domain", consumer.getPropertyValue("trustedObjectPackages"));
            ArgumentCaptor<Runnable> callbacks = ArgumentCaptor.forClass(Runnable.class);
            verify(application).invokeLater(callbacks.capture());
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)));
            callbacks.getValue().run();
            captured.getValue().redo();
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
            verify(application, times(2)).invokeLater(callbacks.capture());
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)), times(2));
            callbacks.getValue().run();
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)), times(3));
            verify(panel, times(3)).updateTargetComponent(module);
            verify(panel, never()).updateTargetComponent(isNull());
            when(context.getIkasanModule()).thenReturn(mock(Module.class));
            assertThrows(UnexpectedUndoException.class, () -> captured.getValue().undo());
            verify(application, times(2)).invokeLater(any(Runnable.class));
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flow)), times(3));
            assertEquals("org.example.debug.domain", consumer.getPropertyValue("trustedObjectPackages"));
        }
    }
}
