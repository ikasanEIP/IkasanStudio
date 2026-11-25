package org.ikasan.studio.intellij.ai;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;
import org.ikasan.studio.ui.StudioUIUtils;

// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
final class StudioAiProposalDialog extends StudioDialogWrapper {
    private final Project project;
    private final StudioAiService service;
    private final JBTextArea status = new StudioAiConnectionText("");
    private final JBLabel completion = new JBLabel();
    private final JPanel feedback = new JPanel(new BorderLayout(0, JBUI.scale(8)));
    private boolean applied;
    private final StudioAiService.Proposal proposal;
    StudioAiProposalDialog(Project project, StudioAiService service, StudioAiService.Proposal proposal) {
        super(project, false);
        this.project = project;
        this.service = service;
        this.proposal = proposal;
        setModal(false);
        setTitle(StudioBundle.message(proposal.fileBased ? "ai.FilePreviewTitle" : "ai.PreviewTitle"));
        setOKButtonText(StudioBundle.message("ai.Apply"));
        init();
    }
    @Override protected JComponent createStudioContent() {
        JBTextArea text = new StudioAiConnectionText(StudioBundle.message("ai.PreviewExplanation") + "\n\n"
                + (proposal.deletionReviewRequired ? StudioBundle.message("ai.DeleteReviewRequired") + "\n\n" : "")
                + (proposal.userCodeReviewRequired ? StudioBundle.message("ai.UserCodeReviewRequired") + "\n\n" : "")
                + String.join("\n", proposal.prepared.summary()) + "\n\n" + proposal.details);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        text.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.PreviewTitle"));
        JBScrollPane pane = new JBScrollPane(text);
        text.setSize(JBUI.scale(720), 1);
        // Fit short proposals; retain scrolling for large operation lists.
        pane.setPreferredSize(new Dimension(JBUI.scale(740),
                Math.min(JBUI.scale(420), text.getPreferredSize().height + JBUI.scale(8))));
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        panel.add(pane, BorderLayout.CENTER);
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setOpaque(false);
        status.setFont(UIManager.getFont("Label.font"));
        status.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.ApplyStatusTitle"));
        status.setVisible(false);
        completion.setIcon(AllIcons.General.InspectionsOK);
        completion.setVisible(false);
        feedback.add(completion, BorderLayout.NORTH);
        feedback.add(status, BorderLayout.CENTER);
        feedback.setVisible(false);
        panel.add(feedback, BorderLayout.SOUTH);
        return panel;
    }
    @Override protected void doOKAction() {
        if (applied) { super.doOKAction(); return; }
        setErrorText(null);
        setOKActionEnabled(false);
        status.setText(StudioBundle.message("ai.Applying"));
        status.setVisible(true);
        feedback.setVisible(true);
        pack();
        try {
            CompletableFuture<Void> generation = service.apply(proposal);
            applied = true;
            setOKButtonText(StudioBundle.message("button.Close"));
            setOKActionEnabled(true);
            getCancelAction().setEnabled(false);
            status.setText(StudioBundle.message("ai.Generating"));
            pack();
            generation.whenComplete((ignored, failure) ->
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) return;
                        String message = StudioBundle.message(failure == null ? (proposal.fileBased ? "ai.FileApplyFinished" : "ai.ApplyFinished") : "ai.ApplyGenerationFailed");
                        if (!isDisposed()) {
                            if (failure == null) {
                                String[] paragraphs = message.split("\\n\\n", 2);
                                completion.setText(paragraphs[0]);
                                completion.setVisible(true);
                                status.setText(paragraphs.length > 1 ? paragraphs[1] : "");
                                status.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
                                feedback.setBorder(JBUI.Borders.compound(
                                        BorderFactory.createLineBorder(JBColor.border()), JBUI.Borders.empty(10)));
                            } else {
                                status.setText(message);
                            }
                            pack();
                        } else if (failure == null) {
                            StudioUIUtils.displayIdeaInfoMessage(project, message);
                        } else {
                            StudioUIUtils.displayIdeaErrorMessage(project, message);
                        }
                    }));
        } catch (RuntimeException failure) {
            if (proposal.feedback != null) proposal.feedback.publish("rejected", failure.getMessage());
            StudioAiImportProposalAction.showFailure(project, failure.getMessage());
            status.setVisible(false);
            feedback.setVisible(false);
            setOKActionEnabled(true);
            setErrorText(failure.getMessage());
        }
    }
    @Override public void doCancelAction() { service.cancel(proposal); super.doCancelAction(); }
    @Override protected void dispose() { service.cancel(proposal); super.dispose(); }
}
