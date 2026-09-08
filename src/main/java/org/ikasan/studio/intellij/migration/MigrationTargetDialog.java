package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import java.awt.*;

final class MigrationTargetDialog extends DialogWrapper {
    private final String current;
    private final ComboBox<String> versions;

    MigrationTargetDialog(Project project, String current, String[] targets) {
        super(project);
        this.current = current;
        versions = new ComboBox<>(targets);
        setTitle(StudioBundle.message("dialog.MigrateIkasanVersion"));
        setOKButtonText(StudioBundle.message("button.PreviewMigration"));
        init();
    }
    String targetVersion() { return (String) versions.getSelectedItem(); }
    @Override public JComponent getPreferredFocusedComponent() { return versions; }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        panel.add(new JBLabel(StudioBundle.message("label.CurrentVersion", current)), BorderLayout.NORTH);
        JBLabel target = new JBLabel(StudioBundle.message("label.TargetIkasanVersion"));
        target.setLabelFor(versions);
        panel.add(target, BorderLayout.WEST);
        panel.add(versions, BorderLayout.CENTER);
        return panel;
    }
}
