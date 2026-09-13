package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.InputValidator;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowClipboardActionsTest {
    @Test void pasteRegistersOneUndoableInsertionAndResynchronizesOnUndoAndRedo() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        DesignerCanvas canvas = mock(DesignerCanvas.class);
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        Flow flow = new Flow(TestFixtures.BASE_META_PACK);
        flow.setName("Pasted Flow");
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getDesignerCanvas()).thenReturn(canvas);
        try (var commands = mockStatic(CommandProcessor.class);
             var undos = mockStatic(UndoManager.class);
             var files = mockStatic(StudioProjectFiles.class);
             var notifications = mockStatic(StudioUIUtils.class);
             var applications = mockStatic(ApplicationManager.class)) {
            CommandProcessor command = mock(CommandProcessor.class);
            commands.when(CommandProcessor::getInstance).thenReturn(command);
            doAnswer(call -> { call.getArgument(1, Runnable.class).run(); return null; })
                    .when(command).executeCommand(eq(project), any(Runnable.class), anyString(), isNull());
            UndoManager undo = mock(UndoManager.class);
            undos.when(() -> UndoManager.getInstance(project)).thenReturn(undo);
            Application application = mock(Application.class);
            // Mockito records this getter invocation to configure the static mock.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
                    .when(application).invokeLater(any(Runnable.class));
            new FlowClipboardActions(project, canvas).insert(module, flow);
            notifications.verify(() -> StudioUIUtils.displayIdeaInfoMessage(project,
                    StudioBundle.message("message.FlowPasted", flow.getIdentity())));
            assertEquals(java.util.List.of(flow), module.getFlows());
            var captured = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undo).undoableActionPerformed(captured.capture());
            files.verify(() -> StudioProjectFiles.refreshCodeFromModelAndCauseRedraw(project, GenerationRequest.moduleStructure(flow)));
            assertTrue(captured.getValue().isGlobal());
            captured.getValue().undo();
            assertTrue(module.getFlows().isEmpty());
            verify(context).resetSelectionAfterDeletion();
            captured.getValue().redo();
            assertEquals(java.util.List.of(flow), module.getFlows());
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.moduleStructure(flow)), times(2));
        }
    }

    @Test void cancellingConflictLeavesDestinationUntouched() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        DesignerCanvas canvas = mock(DesignerCanvas.class);
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        Flow existing = new Flow(TestFixtures.BASE_META_PACK);
        existing.setName("Same Name");
        module.getFlows().add(existing);
        Flow pasted = new Flow(TestFixtures.BASE_META_PACK);
        pasted.setName("Same Name");
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(context.getDesignerCanvas()).thenReturn(canvas);
        try (var messages = mockStatic(Messages.class); var commands = mockStatic(CommandProcessor.class)) {
            new FlowClipboardActions(project, canvas).insert(module, pasted);
            messages.verify(() -> Messages.showInputDialog(eq(project),
                    eq(StudioBundle.message("message.PasteFlowName")), eq(StudioBundle.message("menu.PasteFlow")),
                    isNull(), eq("Same Name Copy"), any(InputValidator.class)));
            assertEquals(java.util.List.of(existing), module.getFlows());
            commands.verifyNoInteractions();
        }
    }
}
