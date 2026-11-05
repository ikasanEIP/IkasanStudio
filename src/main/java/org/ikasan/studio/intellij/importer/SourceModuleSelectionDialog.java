package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity;
import org.ikasan.studio.intellij.project.ImportDialogWrapper;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import java.awt.*;

/** Explicit reactor-module choice before any source recommendations are populated. */
final class SourceModuleSelectionDialog extends ImportDialogWrapper {
    private final SourceCheckoutIdentity.Inspection inspection;
    private final ComboBox<String> modules = new ComboBox<>();
    SourceModuleSelectionDialog(Project project, Component parent, SourceCheckoutIdentity.Inspection inspection) {
        super(project, parent);
        this.inspection = inspection;
        setTitle(StudioBundle.message("sourceImport.identity.chooseTitle"));
        setOKButtonText(StudioBundle.message("sourceImport.identity.chooseAction"));
        for (var candidate : inspection.candidates()) modules.addItem(
                (candidate.directory().isEmpty() ? "." : candidate.directory()) + " — "
                        + (candidate.names().isEmpty() ? StudioBundle.message("sourceImport.identity.unknownName") : String.join(", ", candidate.names()))
                        + (candidate.artifactId().isBlank() ? "" : " (" + candidate.artifactId() + ")"));
        init();
    }
    String selectedDirectory() { return inspection.candidates().get(modules.getSelectedIndex()).directory(); }
    @Override protected JComponent createCenterPanel() {
        var panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        var help = new JBTextArea(StudioBundle.message("sourceImport.identity.chooseHelp", inspection.expected().isBlank()
                ? StudioBundle.message("sourceImport.identity.unknownName") : inspection.expected()));
        help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false); help.setFocusable(false);
        help.setFont(com.intellij.util.ui.UIUtil.getLabelFont());
        panel.add(help, BorderLayout.NORTH);
        var label = new JBLabel(StudioBundle.message("sourceImport.identity.sourceModule")); label.setLabelFor(modules);
        var row = new JPanel(new BorderLayout(JBUI.scale(8), 0)); row.add(label, BorderLayout.WEST); row.add(modules, BorderLayout.CENTER);
        panel.add(row, BorderLayout.CENTER); panel.setPreferredSize(JBUI.size(700, 130)); return panel;
    }
    @Override public JComponent getPreferredFocusedComponent() { return modules; }
}
