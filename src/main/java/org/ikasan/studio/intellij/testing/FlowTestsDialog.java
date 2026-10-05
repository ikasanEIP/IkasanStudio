package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Module-level flow selection; large modules scroll instead of stretching the dialog. */
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
@SuppressWarnings("SplitModeApiUsage")
final class FlowTestsDialog extends DialogWrapper {
    private final List<JBCheckBox> choices = new ArrayList<>();
    private final com.intellij.ui.components.JBCheckBox regenerateSupport = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.regenerateSupport"), false);
    private final com.intellij.ui.components.JBCheckBox refreshProperties = new com.intellij.ui.components.JBCheckBox(
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


    FlowTestsDialog(Project project, String[] names, java.util.Set<String> ftpFlows, java.util.Set<String> smtpFlows, java.util.Set<String> sftpFlows) {
        super(project);
        this.sftpFlows = java.util.Set.copyOf(sftpFlows);
        localSftp.setToolTipText(StudioBundle.message("flowTest.localSftp.help"));
        localSftp.addActionListener(e -> {
            if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
        });
        this.smtpFlows = java.util.Set.copyOf(smtpFlows);
        this.ftpFlows = java.util.Set.copyOf(ftpFlows);
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
        for (String name : names) {
            var choice = new JBCheckBox(name, true);
            choice.addActionListener(e -> updateSelection());
            choices.add(choice);
        }
        setTitle(StudioBundle.message("flowTest.batchTitle"));
        setOKButtonText(StudioBundle.message("flowTest.generate"));
        init();
        updateSelection();
    }
    private void updateSelection() {
        setOKActionEnabled(choices.stream().anyMatch(AbstractButton::isSelected));
        localSmtp.setEnabled(selectedFlows().stream().anyMatch(smtpFlows::contains));
        localSftp.setEnabled(selectedFlows().stream().anyMatch(sftpFlows::contains));
        localFtp.setEnabled(selectedFlows().stream().anyMatch(ftpFlows::contains));
        if (staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()) regenerateSupport.setSelected(true);
        regenerateSupport.setEnabled(!(staleSupport || useLocalFtp() || useLocalSmtp() || useLocalSftp()));
    }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        panel.add(new JBLabel("<html><body style='width: 420px'>"
                + StringUtil.escapeXmlEntities(StudioBundle.message("flowTest.batchExplanation")) + "</body></html>"), BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        choices.forEach(rows::add);
        JBScrollPane scroll = new JBScrollPane(rows);
        int height = choices.isEmpty() ? JBUI.scale(28) : choices.get(0).getPreferredSize().height;
        scroll.setPreferredSize(new Dimension(JBUI.scale(440), height * Math.min(8, Math.max(1, choices.size())) + JBUI.scale(8)));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton all = new JButton(StudioBundle.message("flowTest.selectAll"));
        JButton none = new JButton(StudioBundle.message("flowTest.selectNone"));
        all.addActionListener(e -> { choices.forEach(c -> c.setSelected(true)); updateSelection(); });
        none.addActionListener(e -> { choices.forEach(c -> c.setSelected(false)); updateSelection(); });
        selection.add(all); selection.add(none);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(selection, BorderLayout.NORTH);
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
        footer.add(options, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }
    List<String> selectedFlows() { return choices.stream().filter(AbstractButton::isSelected).map(AbstractButton::getText).toList(); }
    @Override public JComponent getPreferredFocusedComponent() { return choices.isEmpty() ? null : choices.get(0); }
}
