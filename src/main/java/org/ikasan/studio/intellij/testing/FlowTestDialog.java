package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import java.awt.BorderLayout;

/** A compact, non-editable flow choice: arbitrary names cannot produce an invalid request. */
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
@SuppressWarnings("SplitModeApiUsage")
final class FlowTestDialog extends DialogWrapper {
    private final ComboBox<String> flows;
    private final com.intellij.ui.components.JBCheckBox regenerateSupport = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.regenerateSupport"), false);
    private final java.util.Set<String> ftpFlows;
    private final com.intellij.ui.components.JBCheckBox localFtp = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.localFtp"), false);
    boolean useLocalFtp() { return localFtp.isEnabled() && localFtp.isSelected(); }
    boolean regenerateSupport() { return regenerateSupport.isSelected() || useLocalFtp(); }


    FlowTestDialog(Project project, String[] names, String selected, java.util.Set<String> ftpFlows) {
        super(project);
        this.ftpFlows = java.util.Set.copyOf(ftpFlows);
        localFtp.setToolTipText(StudioBundle.message("flowTest.localFtp.help"));
        localFtp.addActionListener(e -> {
            if (useLocalFtp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!useLocalFtp());
        });
        flows = new ComboBox<>(names);
        if (selected != null) flows.setSelectedItem(selected);
        localFtp.setEnabled(ftpFlows.contains(selectedFlow()));
        flows.addActionListener(e -> {
            localFtp.setEnabled(ftpFlows.contains(selectedFlow()));
            regenerateSupport.setEnabled(!useLocalFtp());
        });
        setTitle(StudioBundle.message("flowTest.title"));
        setOKButtonText(StudioBundle.message("flowTest.generate"));
        init();
    }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        panel.add(new JBLabel("<html><body style='width: 420px'>"
                + StringUtil.escapeXmlEntities(StudioBundle.message("flowTest.explanation")) + "</body></html>"), BorderLayout.NORTH);
        JPanel choice = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        JBLabel label = new JBLabel(StudioBundle.message("flowTest.flow"));
        label.setLabelFor(flows);
        choice.add(label, BorderLayout.WEST);
        choice.add(flows, BorderLayout.CENTER);
        panel.add(choice, BorderLayout.CENTER);
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        if (!ftpFlows.isEmpty()) options.add(localFtp);
        options.add(regenerateSupport);
        panel.add(options, BorderLayout.SOUTH);
        return panel;
    }
    @Override public JComponent getPreferredFocusedComponent() { return flows; }
    String selectedFlow() { return (String) flows.getSelectedItem(); }
}
