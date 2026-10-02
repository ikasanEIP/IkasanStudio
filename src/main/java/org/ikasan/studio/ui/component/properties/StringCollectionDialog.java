package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import org.ikasan.studio.core.model.StringCollectionValues;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.*;

/** Edits string entries without comma splitting, value coercion or silent duplicate-key replacement. */
final class StringCollectionDialog extends DialogWrapper {
    private final boolean map;
    private final DefaultTableModel rows;
    private final JBTable table;
    private final JBCheckBox unset = new JBCheckBox(StudioBundle.message("collection.Unset"));

    StringCollectionDialog(Project project, String name, Class<?> type, Object value) {
        super(project);
        map = type == Map.class;
        setTitle(StudioBundle.message("collection.Title", name));
        rows = new DefaultTableModel(map ? new String[]{StudioBundle.message("collection.Key"), StudioBundle.message("collection.Value")}
                : new String[]{StudioBundle.message("collection.Value")}, 0);
        if (value instanceof Map<?, ?> entries) entries.forEach((k, v) -> rows.addRow(new Object[]{k, v}));
        if (value instanceof List<?> entries) entries.forEach(v -> rows.addRow(new Object[]{v}));
        table = new JBTable(rows);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.getAccessibleContext().setAccessibleName(name);
        unset.setSelected(value == null);
        unset.addActionListener(e -> table.setEnabled(!unset.isSelected()));
        table.setEnabled(!unset.isSelected());
        init();
        if (value instanceof String) setErrorText(StudioBundle.message("collection.InvalidLegacy"));
    }
    @Override protected JComponent createCenterPanel() {
        var panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JBLabel(StudioBundle.message("collection.Guidance")), BorderLayout.NORTH);
        panel.add(ToolbarDecorator.createDecorator(table)
                .setAddAction(b -> {
                    finishEditing();
                    unset.setSelected(false);
                    table.setEnabled(true);
                    rows.addRow(map ? new Object[]{"", ""} : new Object[]{""});
                    int row = rows.getRowCount() - 1;
                    table.setRowSelectionInterval(row, row);
                    table.editCellAt(row, 0);
                })
                .setRemoveAction(b -> {
                    finishEditing();
                    int[] selected = table.getSelectedRows();
                    for (int i = selected.length - 1; i >= 0; i--) rows.removeRow(selected[i]);
                }).disableUpDownActions().createPanel(), BorderLayout.CENTER);
        panel.add(unset, BorderLayout.SOUTH);
        panel.setPreferredSize(com.intellij.util.ui.JBUI.size(520, 260));
        return panel;
    }
    private void finishEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }
    @Override protected ValidationInfo doValidate() {
        finishEditing();
        if (!unset.isSelected() && map) {
            Set<String> keys = new HashSet<>();
            for (int i = 0; i < rows.getRowCount(); i++)
                if (!keys.add((String) rows.getValueAt(i, 0)))
                    return new ValidationInfo(StudioBundle.message("collection.DuplicateKey"), table);
        }
        return null;
    }
    Object value() {
        finishEditing();
        if (unset.isSelected()) return null;
        if (map) {
            Map<String, String> result = new LinkedHashMap<>();
            for (int i = 0; i < rows.getRowCount(); i++) result.put((String) rows.getValueAt(i, 0), (String) rows.getValueAt(i, 1));
            return StringCollectionValues.normalize(Map.class, result);
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < rows.getRowCount(); i++) result.add((String) rows.getValueAt(i, 0));
        return StringCollectionValues.normalize(List.class, result);
    }
}
