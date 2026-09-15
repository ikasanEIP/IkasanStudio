package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.analysis.ModelReferenceReplacement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.intellij.psi.StudioPsiUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

final class ReplaceReferencesDialog extends DialogWrapper {
    private final Project project;
    private final Module module;
    private final JBTextField find = new JBTextField();
    private final JBTextField replacement = new JBTextField();
    private final DefaultTableModel rows = new DefaultTableModel(new Object[]{
            StudioBundle.message("replaceReferences.Include"), StudioBundle.message("replaceReferences.Flow"),
            StudioBundle.message("replaceReferences.Component"), StudioBundle.message("replaceReferences.Property"),
            StudioBundle.message("replaceReferences.Before"), StudioBundle.message("replaceReferences.After")}, 0) {
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
    };
    private List<ModelReferenceReplacement.Change> changes = List.of();

    ReplaceReferencesDialog(Project project, Module module) {
        super(project, false);
        this.project = project;
        this.module = module;
        setTitle(StudioBundle.message("action.IkasanStudio.ReplaceReferences.text"));
        setOKButtonText(StudioBundle.message("replaceReferences.Apply"));
        init();
        setOKActionEnabled(false);
        DocumentAdapter invalidate = new DocumentAdapter() {
            @Override protected void textChanged(@NotNull DocumentEvent event) {
                changes = List.of();
                rows.setRowCount(0);
                setOKActionEnabled(false);
                setErrorText(null);
            }
        };
        find.getDocument().addDocumentListener(invalidate);
        replacement.getDocument().addDocumentListener(invalidate);
        rows.addTableModelListener(event -> setOKActionEnabled(!selectedChanges().isEmpty()));
    }

    @Override public JComponent getPreferredFocusedComponent() { return find; }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        JPanel fields = new JPanel(new GridLayout(0, 2, JBUI.scale(8), JBUI.scale(8)));
        JBLabel findLabel = new JBLabel(StudioBundle.message("replaceReferences.Find"));
        findLabel.setLabelFor(find);
        fields.add(findLabel);
        fields.add(withClassChooser(find));
        JBLabel replaceLabel = new JBLabel(StudioBundle.message("replaceReferences.ReplaceWith"));
        replaceLabel.setLabelFor(replacement);
        fields.add(replaceLabel);
        fields.add(withClassChooser(replacement));
        JButton preview = new JButton(StudioBundle.message("replaceReferences.Preview"));
        preview.addActionListener(event -> preview());
        fields.add(preview);
        JPanel header = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JBTextArea explanation = new JBTextArea(StudioBundle.message("replaceReferences.Explanation"));
        explanation.setEditable(false);
        explanation.setOpaque(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setFont(find.getFont());
        header.add(explanation, BorderLayout.NORTH);
        header.add(fields, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);
        JBTable table = new JBTable(rows);
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(70));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 1; i < 6; i++) table.getColumnModel().getColumn(i).setPreferredWidth(JBUI.scale(i < 4 ? 155 : 280));
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("replaceReferences.Preview"));
        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setPreferredSize(JBUI.size(900, 300));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent withClassChooser(JBTextField field) {
        JPanel input = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        input.add(field, BorderLayout.CENTER);
        JButton chooseClass = new JButton(StudioBundle.message("button.ChooseClass"));
        chooseClass.addActionListener(event -> {
            String selected = StudioPsiUtils.chooseClassQualifiedName(project, StudioBundle.message("dialog.ChooseClass"));
            if (selected != null) field.setText(selected);
        });
        input.add(chooseClass, BorderLayout.EAST);
        return input;
    }

    private void preview() {
        rows.setRowCount(0);
        changes = List.of();
        try {
            changes = ModelReferenceReplacement.preview(module, find.getText().trim(), replacement.getText().trim());
            for (var change : changes) rows.addRow(new Object[]{true, change.flow().getIdentity(),
                    change.element().getIdentity(), change.key(), String.valueOf(change.before()), String.valueOf(change.after())});
            setErrorText(changes.isEmpty() ? StudioBundle.message("replaceReferences.NoMatches") : null);
        } catch (IllegalArgumentException failure) {
            setErrorText(StudioBundle.message("replaceReferences.InvalidNames"));
        }
        setOKActionEnabled(!selectedChanges().isEmpty());
    }

    private List<ModelReferenceReplacement.Change> selectedChanges() {
        List<ModelReferenceReplacement.Change> selected = new ArrayList<>();
        for (int i = 0; i < rows.getRowCount() && i < changes.size(); i++) {
            if (Boolean.TRUE.equals(rows.getValueAt(i, 0))) selected.add(changes.get(i));
        }
        return List.copyOf(selected);
    }

    @Override protected void doOKAction() {
        try {
            ReplaceReferencesAction.apply(project, module, selectedChanges());
            super.doOKAction();
        } catch (RuntimeException failure) {
            setErrorText(StudioBundle.message("message.ReplaceReferencesApplyFailed"));
        }
    }
}
