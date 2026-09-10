package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Native controls avoid the checkbox-message dialog's limited HTML message renderer. */
final class RegenerateUserClassDialog extends DialogWrapper {
    record AffectedClass(String flow, String type, String className) {}
    private final String message;
    private final List<AffectedClass> affected;
    private final JBCheckBox backup = new JBCheckBox(
            StudioBundle.message("checkbox.BackupUserImplementedClassBeforeOverwrite"), true);

    RegenerateUserClassDialog(Project project, String message, List<AffectedClass> affected) {
        super(project, false);
        this.message = message;
        this.affected = List.copyOf(affected);
        setTitle(StudioBundle.message("dialog.ConfirmRegenerateUserImplementedClass"));
        setOKButtonText(Messages.getYesButton());
        setCancelButtonText(Messages.getNoButton());
        init();
    }

    boolean isBackupSelected() { return backup.isSelected(); }

    @Override
    protected JComponent createCenterPanel() {
        return buildContent(message, affected, backup);
    }

    @Override
    public JComponent getPreferredFocusedComponent() { return getButton(getOKAction()); }

    static JComponent buildContent(String message, List<AffectedClass> affected, JCheckBox backup) {
        JPanel details = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = JBUI.insetsBottom(12);
        details.add(PropertyDialogLayout.wrappedText(message, 520), c);
        c.gridwidth = 1;
        for (AffectedClass item : affected) {
            PropertyDialogLayout.addRow(details, c, "Flow:", item.flow());
            PropertyDialogLayout.addRow(details, c, "Component (" + item.type() + "):", item.className());
        }
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = JBUI.insetsTop(12);
        details.add(new JBLabel(StudioBundle.message("label.ContinueRegeneration")), c);
        return PropertyDialogLayout.wrap(details, backup);
    }
}
