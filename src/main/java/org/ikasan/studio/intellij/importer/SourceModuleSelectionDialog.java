package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.*;
import javax.swing.*;
import javax.swing.JPanel;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity;
import org.ikasan.studio.intellij.project.ImportDialogWrapper;
import org.ikasan.studio.ui.StudioBundle;

/** Explicit reactor-module choice before any source recommendations are populated. */
final class SourceModuleSelectionDialog extends ImportDialogWrapper {
    private final SourceCheckoutIdentity.Inspection inspection;
    private final ComboBox<String> modules = new ComboBox<>();
    SourceModuleSelectionDialog(Project project, Component parent, SourceCheckoutIdentity.Inspection inspection) {
        super(project, parent);
        this.inspection = inspection;
        setTitle(StudioBundle.message("sourceImport.identity.chooseTitle"));
        setOKButtonText(StudioBundle.message("sourceImport.identity.chooseAction"));
        for (Candidate candidate : inspection.candidates()) modules.addItem(
                (candidate.directory().isEmpty() ? "." : candidate.directory()) + " — "
                        + (candidate.names().isEmpty() ? StudioBundle.message("sourceImport.identity.unknownName") : String.join(", ", candidate.names()))
                        + (candidate.artifactId().isBlank() ? "" : " (" + candidate.artifactId() + ")"));
        init();
    }
    String selectedDirectory() { return inspection.candidates().get(modules.getSelectedIndex()).directory(); }
    @Override protected JComponent createStudioContent() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JBTextArea help = new JBTextArea(StudioBundle.message("sourceImport.identity.chooseHelp", inspection.expected().isBlank()
                ? StudioBundle.message("sourceImport.identity.unknownName") : inspection.expected()));
        help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false); help.setFocusable(false);
        help.setFont(UIUtil.getLabelFont());
        panel.add(help, BorderLayout.NORTH);
        JBLabel label = new JBLabel(StudioBundle.message("sourceImport.identity.sourceModule")); label.setLabelFor(modules);
        JPanel row = new JPanel(new BorderLayout(JBUI.scale(8), 0)); row.add(label, BorderLayout.WEST); row.add(modules, BorderLayout.CENTER);
        panel.add(row, BorderLayout.CENTER); panel.setPreferredSize(JBUI.size(700, 130)); return panel;
    }
    @Override public JComponent getPreferredFocusedComponent() { return modules; }
}
