package org.ikasan.studio.intellij.project;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.NotNull;

public final class RuntimeImportReportDialog extends ImportDialogWrapper {
    public RuntimeImportReportDialog(Project project, Component parent) {
        super(project, parent);
        setTitle(StudioBundle.message("import.diagnostics.title"));
        setOKButtonText(StudioBundle.message("button.Close"));
        reportText = RuntimeImportDiagnostics.get(project).text();
        init();
    }
    private final String reportText;
    @Override protected Action @NotNull [] createActions() { return new Action[] {getOKAction()}; }
    @Override protected JComponent createStudioContent() {
        String[] pages = reportText.split("\\n(?=PAGE [0-9]+ —)");
        JBTextArea text = new JBTextArea(pages[0], 25, 85);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
        JBScrollPane scroll = new JBScrollPane(text);
        scroll.setPreferredSize(JBUI.size(760, 500)); scroll.setMinimumSize(new Dimension(0, 0));
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ComboBox<Integer> selector = new ComboBox<>();
        for (int i = 1; i <= pages.length; i++) selector.addItem(i);
        JBLabel label = new JBLabel(StudioBundle.message("import.diagnostics.page"));
        label.setLabelFor(selector); controls.add(label); controls.add(selector);
        selector.addActionListener(event -> { text.setText(pages[selector.getSelectedIndex()]); text.setCaretPosition(0); });
        JButton copy = new JButton(StudioBundle.message("import.diagnostics.copy"));
        copy.addActionListener(event -> CopyPasteManager.getInstance().setContents(new StringSelection(reportText)));
        controls.add(copy);
        panel.add(controls, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}
