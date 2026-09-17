package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;

final class StudioAiProposalDialog extends DialogWrapper {
    private final StudioAiService service;
    private final StudioAiService.Proposal proposal;
    StudioAiProposalDialog(Project project, StudioAiService service, StudioAiService.Proposal proposal) {
        super(project, false);
        this.service = service;
        this.proposal = proposal;
        setModal(false);
        setTitle(StudioBundle.message("ai.PreviewTitle"));
        setOKButtonText(StudioBundle.message("ai.Apply"));
        init();
    }
    @Override protected JComponent createCenterPanel() {
        JBTextArea text = new JBTextArea(StudioBundle.message("ai.PreviewExplanation") + "\n\n"
                + String.join("\n", proposal.prepared.summary()) + "\n\n" + proposal.details);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        text.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.PreviewTitle"));
        JBScrollPane pane = new JBScrollPane(text);
        pane.setPreferredSize(JBUI.size(720, 480));
        return pane;
    }
    @Override protected void doOKAction() {
        try { service.apply(proposal); super.doOKAction(); }
        catch (RuntimeException failure) { setErrorText(failure.getMessage()); }
    }
    @Override public void doCancelAction() { service.cancel(proposal); super.doCancelAction(); }
    @Override protected void dispose() { service.cancel(proposal); super.dispose(); }
}
