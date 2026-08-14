package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.model.StudioPsiUtils;

/**
 * Reverses (or re-applies) the in-memory model change made by a single "delete component"/"delete flow"
 * gesture, then resyncs generated code and the canvas - so the deletion participates in IntelliJ's normal
 * Undo/Redo (Ctrl+Z / Ctrl+Shift+Z) like any other IDE edit, even though the underlying {@code Module}/
 * {@code Flow} model is a plain object graph, not a Document/PSI that IntelliJ tracks automatically.
 * <p>
 * Implements {@link UndoableAction} directly (rather than extending {@code BasicUndoableAction}) because
 * {@code BasicUndoableAction.isGlobal()} is hardcoded to return {@code false} regardless of constructor -
 * it never actually produces a "global" action. This class overrides {@link #isGlobal()} to return
 * {@code true}, which is what's needed here: the Designer Canvas is its own {@code FileEditor}, not
 * associated with any of the files this delete touches (model.json, generated sources), so a non-global
 * action with no affected documents would never be recognised as available by Edit > Undo while the
 * canvas has focus - which is exactly the bug this fixes.
 */
public class DeleteComponentUndoableAction implements UndoableAction {
    private static final Logger LOG = Logger.getInstance("#DeleteComponentUndoableAction");

    private final Project project;
    private final Runnable undoModel;
    private final Runnable redoModel;

    public DeleteComponentUndoableAction(Project project, Runnable undoModel, Runnable redoModel) {
        this.project = project;
        this.undoModel = undoModel;
        this.redoModel = redoModel;
    }

    @Override
    public void undo() {
        applyModelChange(undoModel);
    }

    @Override
    public void redo() {
        applyModelChange(redoModel);
    }

    @Override
    public DocumentReference[] getAffectedDocuments() {
        return null;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    /**
     * Mutates the model and repaints the canvas immediately (both plain, synchronous, non-transactional
     * operations, safe to run from inside IntelliJ's undo/redo processing). The JSON-and-generated-code
     * resync is deferred one EDT tick via {@code invokeLater}: {@code refreshCodeFromModel} opens its own
     * {@code CommandProcessor} command, and starting a new command from inside an in-progress undo/redo is
     * the kind of reentrancy IntelliJ doesn't support - the deferred call runs after undo/redo processing
     * has fully returned, exactly like it would for any ordinary (non-undo) model edit.
     */
    private void applyModelChange(Runnable modelChange) {
        try {
            modelChange.run();
            StudioPsiUtils.causeRedraw(project);
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    StudioPsiUtils.refreshCodeFromModel(project);
                } catch (Exception e) {
                    LOG.warn("STUDIO: Failed to resync generated code after an undo/redo of a delete.", e);
                }
            });
        } catch (Exception e) {
            LOG.warn("STUDIO: Failed to undo/redo a delete.", e);
        }
    }
}
