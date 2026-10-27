package org.ikasan.studio.intellij.project;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import java.awt.*;

public final class RuntimeImportReportDialog extends ImportDialogWrapper {
    public RuntimeImportReportDialog(Project project, Component parent) {
        super(project, parent);
        setTitle(StudioBundle.message("import.diagnostics.title"));
        setOKButtonText(StudioBundle.message("button.Close"));
        reportText = RuntimeImportDiagnostics.get(project).text();
        init();
    }
    private final String reportText;
    @Override protected Action[] createActions() { return new Action[] {getOKAction()}; }
    @Override protected JComponent createCenterPanel() {
        String[] pages = reportText.split("\\n(?=PAGE [0-9]+ —)");
        var text = new JBTextArea(pages[0], 25, 85);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
        var scroll = new JBScrollPane(text);
        scroll.setPreferredSize(JBUI.size(760, 500)); scroll.setMinimumSize(new Dimension(0, 0));
        var panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        var controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var selector = new com.intellij.openapi.ui.ComboBox<Integer>();
        for (int i = 1; i <= pages.length; i++) selector.addItem(i);
        var label = new com.intellij.ui.components.JBLabel(StudioBundle.message("import.diagnostics.page"));
        label.setLabelFor(selector); controls.add(label); controls.add(selector);
        selector.addActionListener(event -> { text.setText(pages[selector.getSelectedIndex()]); text.setCaretPosition(0); });
        var copy = new JButton(StudioBundle.message("import.diagnostics.copy"));
        copy.addActionListener(event -> com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(new java.awt.datatransfer.StringSelection(reportText)));
        controls.add(copy);
        panel.add(controls, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}
