package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

final class MigrationTargetDialog extends DialogWrapper {
    private final String current;
    private final ComboBox<String> versions;

    MigrationTargetDialog(Project project, String current, String[] targets) {
        super(project);
        this.current = current;
        versions = new ComboBox<>(targets);
        setTitle("Migrate Ikasan version");
        setOKButtonText("Preview migration");
        init();
    }
    String targetVersion() { return (String) versions.getSelectedItem(); }
    @Override public JComponent getPreferredFocusedComponent() { return versions; }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        panel.add(new JBLabel("Current version: " + current), BorderLayout.NORTH);
        JBLabel target = new JBLabel("Target Ikasan version:");
        target.setLabelFor(versions);
        panel.add(target, BorderLayout.WEST);
        panel.add(versions, BorderLayout.CENTER);
        return panel;
    }
}
