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
    private final java.util.Set<String> ftpFlows;
    private final com.intellij.ui.components.JBCheckBox localFtp = new com.intellij.ui.components.JBCheckBox(
            StudioBundle.message("flowTest.localFtp"), false);
    boolean useLocalFtp() { return localFtp.isEnabled() && localFtp.isSelected(); }
    boolean regenerateSupport() { return regenerateSupport.isSelected() || useLocalFtp(); }


    FlowTestsDialog(Project project, String[] names, java.util.Set<String> ftpFlows) {
        super(project);
        this.ftpFlows = java.util.Set.copyOf(ftpFlows);
        localFtp.setToolTipText(StudioBundle.message("flowTest.localFtp.help"));
        localFtp.addActionListener(e -> {
            if (useLocalFtp()) regenerateSupport.setSelected(true);
            regenerateSupport.setEnabled(!useLocalFtp());
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
        localFtp.setEnabled(selectedFlows().stream().anyMatch(ftpFlows::contains));
        regenerateSupport.setEnabled(!useLocalFtp());
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
        if (!ftpFlows.isEmpty()) options.add(localFtp);
        options.add(regenerateSupport);
        footer.add(options, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }
    List<String> selectedFlows() { return choices.stream().filter(AbstractButton::isSelected).map(AbstractButton::getText).toList(); }
    @Override public JComponent getPreferredFocusedComponent() { return choices.isEmpty() ? null : choices.get(0); }
}
