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
    FlowTestsDialog(Project project, String[] names) {
        super(project);
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
    private void updateSelection() { setOKActionEnabled(choices.stream().anyMatch(AbstractButton::isSelected)); }
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
        panel.add(selection, BorderLayout.SOUTH);
        return panel;
    }
    List<String> selectedFlows() { return choices.stream().filter(AbstractButton::isSelected).map(AbstractButton::getText).toList(); }
    @Override public JComponent getPreferredFocusedComponent() { return choices.isEmpty() ? null : choices.get(0); }
}
