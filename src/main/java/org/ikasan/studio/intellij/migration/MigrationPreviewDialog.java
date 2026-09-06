package org.ikasan.studio.intellij.migration;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestPanel;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.migration.MigrationWorkspace;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

final class MigrationPreviewDialog extends DialogWrapper {
    private final String report;
    private final List<MigrationWorkspace.Change> changes;
    private final Project project;
    private final JBCheckBox compile = new JBCheckBox("Build the project after applying", true);

    MigrationPreviewDialog(Project project, String title, String report, List<MigrationWorkspace.Change> changes, boolean canApply) {
        super(project);
        this.project = project;
        this.report = report;
        this.changes = changes;
        setTitle(title);
        setOKButtonText("Apply migration");
        init();
        setOKActionEnabled(canApply);
        compile.setEnabled(canApply);
    }

    boolean shouldCompile() { return compile.isSelected(); }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        panel.setPreferredSize(JBUI.size(960, 650));
        JBTabbedPane tabs = new JBTabbedPane();
        JBTextArea summary = new JBTextArea(report);
        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        tabs.addTab("Migration report", new JBScrollPane(summary));
        List<MigrationWorkspace.Change> changed = changes.stream().filter(c -> !Objects.equals(c.before(), c.after())).toList();
        if (!changed.isEmpty()) {
            JBList<String> paths = new JBList<>(changed.stream().map(MigrationWorkspace.Change::path).toArray(String[]::new));
            paths.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            DiffRequestPanel diff = DiffManager.getInstance().createRequestPanel(project, getDisposable(), null);
            paths.addListSelectionListener(event -> {
                int index = paths.getSelectedIndex();
                if (index < 0) return;
                var change = changed.get(index);
                diff.setRequest(new SimpleDiffRequest(change.path(),
                        DiffContentFactory.getInstance().create(project, change.beforeText()),
                        DiffContentFactory.getInstance().create(project, change.afterText()),
                        change.before() == null ? "New file" : "Current", change.after() == null ? "Delete file" : "Proposed"));
            });
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JBScrollPane(paths), diff.getComponent());
            split.setDividerLocation(JBUI.scale(300));
            tabs.addTab("File changes (" + changed.size() + ")", split);
            paths.setSelectedIndex(0);
        }
        panel.add(tabs, BorderLayout.CENTER);
        panel.add(compile, BorderLayout.SOUTH);
        return panel;
    }
}
