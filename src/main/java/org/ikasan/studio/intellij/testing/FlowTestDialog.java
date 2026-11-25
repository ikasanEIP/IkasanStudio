package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.util.Set;
import javax.swing.*;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;

/** A compact, non-editable flow choice: arbitrary names cannot produce an invalid request. */
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
final class FlowTestDialog extends StudioDialogWrapper {
    private final ComboBox<String> flows;
    private final JBCheckBox regenerateSupport = new JBCheckBox(
            StudioBundle.message("flowTest.regenerateSupport"), false);
    private final JBCheckBox refreshProperties = new JBCheckBox(
            StudioBundle.message("flowTest.refreshProperties"), false);
    void setRefreshProperties(boolean selected) { refreshProperties.setSelected(selected); }
    boolean refreshProperties() { return refreshProperties.isSelected(); }
    private boolean staleSupport;
    private final JBLabel staleNotice = new JBLabel("<html><body style='width: 420px'>"
            + StringUtil.escapeXmlEntities(StudioBundle.message("flowTest.staleSupport")) + "</body></html>");

    void setSupportStale(boolean stale) {
        staleSupport = stale;
        staleNotice.setVisible(stale);
        if (stale) regenerateSupport.setSelected(true);
        regenerateSupport.setEnabled(!(stale || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        if (getWindow() != null) getWindow().pack();
    }

    private final Set<String> sftpFlows;
    private final JBCheckBox localSftp = new JBCheckBox(
            StudioBundle.message("flowTest.localSftp"), true);
    boolean useLocalSftp() { return localSftp.isEnabled() && localSftp.isSelected(); }
    private final Set<String> ftpFlows;
    private final JBCheckBox localFtp = new JBCheckBox(
            StudioBundle.message("flowTest.localFtp"), true);
    private final Set<String> smtpFlows;
    private final JBCheckBox localSmtp = new JBCheckBox(
            StudioBundle.message("flowTest.localSmtp"), true);
    boolean useLocalSmtp() { return localSmtp.isEnabled() && localSmtp.isSelected(); }
    boolean useLocalFtp() { return localFtp.isEnabled() && localFtp.isSelected(); }
    boolean regenerateSupport() { return regenerateSupport.isSelected() || useLocalFtp() || useLocalSmtp() || useLocalSftp(); }


    FlowTestDialog(Project project, String[] names, String selected, Set<String> ftpFlows, Set<String> smtpFlows, Set<String> sftpFlows) {
        super(project);
        this.sftpFlows = Set.copyOf(sftpFlows);
        localSftp.setToolTipText(StudioBundle.message("flowTest.localSftp.help"));
        localSftp.addActionListener(e -> {
            if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        this.smtpFlows = Set.copyOf(smtpFlows);
        this.ftpFlows = Set.copyOf(ftpFlows);
        localSmtp.setToolTipText(StudioBundle.message("flowTest.localSmtp.help"));
        localSmtp.addActionListener(e -> {
            if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        localFtp.setToolTipText(StudioBundle.message("flowTest.localFtp.help"));
        localFtp.addActionListener(e -> {
            if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
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
        if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
        regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
    }
    @Override protected JComponent createStudioContent() {
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
        refreshProperties.setToolTipText(StudioBundle.message("flowTest.refreshProperties.help"));
        options.add(refreshProperties);
        staleNotice.setVisible(staleSupport);
        options.add(staleNotice);
        panel.add(options, BorderLayout.SOUTH);
        return panel;
    }
    @Override public JComponent getPreferredFocusedComponent() { return flows; }
    String selectedFlow() { return (String) flows.getSelectedItem(); }
}
