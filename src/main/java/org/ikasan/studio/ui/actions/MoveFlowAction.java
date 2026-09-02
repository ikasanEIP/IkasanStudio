package org.ikasan.studio.ui.actions;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.core.generation.GenerationRequest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

/**
 * Moves a single Flow one position up or down in the module's flow list - a one-shot, undoable structural edit
 * (see {@link DeleteComponentAction}'s whole-flow removal for the same pattern), letting the user manually
 * order flows exactly as they like (e.g. to sit a JMS-connected pair adjacent to one another) rather than any
 * automatic heuristic guessing on their behalf.
 */
public class MoveFlowAction implements ActionListener {
    private final Project project;
    private final Flow flow;
    private final boolean moveUp;

    public MoveFlowAction(Project project, Flow flow, boolean moveUp) {
        this.project = project;
        this.flow = flow;
        this.moveUp = moveUp;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Module ikasanModule = project.getService(UiContext.class).getIkasanModule();
        if (ikasanModule == null || ikasanModule.getFlows() == null) {
            return;
        }
        List<Flow> flows = ikasanModule.getFlows();
        int index = flows.indexOf(flow);
        int targetIndex = index + (moveUp ? -1 : 1);
        if (index < 0 || targetIndex < 0 || targetIndex >= flows.size()) {
            // Menu construction already only offers this when applicable - defensive no-op if the model
            // changed between the menu being built and the item being clicked.
            return;
        }

        CommandProcessor.getInstance().executeCommand(project, () -> {
            // Swapping two adjacent positions is its own inverse - the same Runnable serves as both undo and
            // redo, unlike DeleteComponentAction's remove/re-insert pair which genuinely differ.
            Runnable swap = () -> Collections.swap(flows, index, targetIndex);
            swap.run();
            UndoManager.getInstance(project).undoableActionPerformed(
                    new DeleteComponentUndoableAction(project, swap, swap, GenerationRequest.moduleStructure(flow)));
            // Kept inside this command so the JSON model save nests into (and is undone/redone as part of)
            // the same undo step as the model mutation above, matching DeleteComponentAction's own whole-flow
            // edit pattern.
            StudioProjectFiles.refreshCodeFromModelAndCauseRedraw(project, GenerationRequest.moduleStructure(null));
        }, StudioBundle.message(moveUp ? "menu.MoveFlowUp" : "menu.MoveFlowDown"), null);
    }
}
