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
    private final java.util.Set<String> sftpFlows;
    private final com.intellij.ui.components.JBCheckBox localSftp = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.localSftp"), true);
    boolean useLocalSftp() { return localSftp.isEnabled() && localSftp.isSelected(); }
    private final java.util.Set<String> ftpFlows;
    private final com.intellij.ui.components.JBCheckBox localFtp = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.localFtp"), true);
    private final java.util.Set<String> smtpFlows;
    private final com.intellij.ui.components.JBCheckBox localSmtp = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.localSmtp"), true);
    boolean useLocalSmtp() { return localSmtp.isEnabled() && localSmtp.isSelected(); }
    boolean useLocalFtp() { return localFtp.isEnabled() && localFtp.isSelected(); }
    boolean regenerateSupport() { return regenerateSupport.isSelected() || useLocalFtp() || useLocalSmtp() || useLocalSftp(); }


    FlowTestDialog(Project project, String[] names, String selected, java.util.Set<String> ftpFlows, java.util.Set<String> smtpFlows, java.util.Set<String> sftpFlows) {
        super(project);
        this.sftpFlows = java.util.Set.copyOf(sftpFlows);
        localSftp.setToolTipText(StudioBundle.message("flowTest.localSftp.help"));
        localSftp.addActionListener(e -> {
            if (useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        this.smtpFlows = java.util.Set.copyOf(smtpFlows);
        this.ftpFlows = java.util.Set.copyOf(ftpFlows);
        localSmtp.setToolTipText(StudioBundle.message("flowTest.localSmtp.help"));
        localSmtp.addActionListener(e -> {
            if (useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        localFtp.setToolTipText(StudioBundle.message("flowTest.localFtp.help"));
        localFtp.addActionListener(e -> {
            if (useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        flows = new ComboBox<>(names);
        if (selected != null) flows.setSelectedItem(selected);
        updateFixtureSelection();
        flows.addActionListener(e -> updateFixtureSelection());
        setTitle(StudioBundle.message("flowTest.title"));
        setOKButtonText(StudioBundle.message("flowTest.generate"));
        init();
    }
    private void updateFixtureSelection() {
        localSmtp.setEnabled(smtpFlows.contains(selectedFlow()));
        localSftp.setEnabled(sftpFlows.contains(selectedFlow()));
        localFtp.setEnabled(ftpFlows.contains(selectedFlow()));
        if (useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
        regenerateSupport.setEnabled(!(useLocalFtp() || useLocalSmtp() || useLocalSftp()));
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
        if (!sftpFlows.isEmpty()) options.add(localSftp);
        if (!ftpFlows.isEmpty()) options.add(localFtp);
        if (!smtpFlows.isEmpty()) options.add(localSmtp);
        options.add(regenerateSupport);
        panel.add(options, BorderLayout.SOUTH);
        return panel;
    }
    @Override public JComponent getPreferredFocusedComponent() { return flows; }
    String selectedFlow() { return (String) flows.getSelectedItem(); }
}
