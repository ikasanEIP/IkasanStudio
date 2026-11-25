package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;

/** Module-level flow selection; large modules scroll instead of stretching the dialog. */
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
final class FlowTestsDialog extends StudioDialogWrapper {
    private final List<JBCheckBox> choices = new ArrayList<>();
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


    FlowTestsDialog(Project project, String[] names, Set<String> ftpFlows, Set<String> smtpFlows, Set<String> sftpFlows) {
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
        for (String name : names) {
            JBCheckBox choice = new JBCheckBox(name, true);
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
    @Override protected JComponent createStudioContent() {
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
