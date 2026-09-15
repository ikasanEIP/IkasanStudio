package org.ikasan.studio.ui.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.analysis.ModelReferenceReplacement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReplaceReferencesAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
    @Override public void update(AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && ready(event.getProject()));
    }
    @Override public void actionPerformed(AnActionEvent event) {
        if (event.getProject() != null) open(event.getProject());
    }

    private static boolean ready(Project project) {
        var context = project.getService(UiContext.class);
        var module = context.getIkasanModule();
        return !project.isDisposed() && module != null && module.isInitialised()
                && !context.isModelPersistenceBlocked() && !context.isMigrationActive()
                && context.getLatestGeneration().isDone();
    }

    public static void open(Project project) {
        if (!ready(project)) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ReplaceReferencesBusy"));
            return;
        }
        var context = project.getService(UiContext.class);
        var panel = context.getPropertiesPanel();
        if (panel != null) {
            if (!panel.confirmSelectionChangeWithPendingEdits(null)) return;
            refreshProperties(project);
        }
        // Applying pending edits can start generation. The preview must wait for that operation.
        if (!ready(project)) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ReplaceReferencesBusy"));
            return;
        }
        new ReplaceReferencesDialog(project, context.getIkasanModule()).show();
    }

    static void apply(Project project, Module module, List<ModelReferenceReplacement.Change> changes) {
        requireCurrent(project, module);
        if (changes.isEmpty()) return;
        GenerationRequest request = changes.stream().map(change -> GenerationRequest.flow(change.flow()))
                .reduce(GenerationRequest.modelOnly(), GenerationRequest::merge);
        // Initialise the undo listener before its command starts (also needed on a fresh project).
        UndoManager undoManager = UndoManager.getInstance(project);
        ModelReferenceReplacement.apply(module, changes, true);
        try { StudioProjectFiles.refreshCodeFromModel(project, request); }
        catch (RuntimeException failure) {
            ModelReferenceReplacement.apply(module, changes, false);
            throw failure;
        }
        // Atomic model-file replacement emits external VFS changes. Keep those outside the model undo command.
        CommandProcessor.getInstance().executeCommand(project, () -> {
            undoManager.undoableActionPerformed(new UndoableAction() {
                @Override public void undo() throws UnexpectedUndoException { change(false); }
                @Override public void redo() throws UnexpectedUndoException { change(true); }
                @Override public DocumentReference[] getAffectedDocuments() { return null; }
                @Override public boolean isGlobal() { return true; }
                private void change(boolean forward) throws UnexpectedUndoException {
                    try {
                        requireCurrent(project, module);
                        ModelReferenceReplacement.apply(module, changes, forward);
                        refreshProperties(project);
                        // Generation opens a command of its own; wait until the undo command has completed.
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed() || project.getService(UiContext.class).getIkasanModule() != module) return;
                            try { StudioProjectFiles.refreshCodeFromModel(project, request); }
                            catch (RuntimeException failure) {
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ReplaceReferencesSaveFailed"));
                            }
                        });
                    } catch (RuntimeException failure) {
                        throw new UnexpectedUndoException(StudioBundle.message("message.ReplaceReferencesStale"));
                    }
                }
            });
            refreshProperties(project);
        }, StudioBundle.message("action.IkasanStudio.ReplaceReferences.text"), null);
    }

    private static void requireCurrent(Project project, Module module) {
        var context = project.getService(UiContext.class);
        if (!ready(project) || context.getIkasanModule() != module
                || (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess())) {
            throw new IllegalStateException(StudioBundle.message("message.ReplaceReferencesStale"));
        }
    }

    private static void refreshProperties(Project project) {
        var context = project.getService(UiContext.class);
        var panel = context.getPropertiesPanel();
        if (panel != null) {
            // Opening the module context menu does not necessarily select a canvas element.
            var target = context.getSelectedComponent();
            if (target == null) target = context.getIkasanModule();
            if (target != null) panel.updateTargetComponent(target);
        }
        StudioProjectFiles.causeRedraw(project);
    }
}
