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
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

final class MigrationPreviewDialog extends DialogWrapper {
    private final com.intellij.openapi.ui.ComboBox<com.intellij.openapi.projectRoots.Sdk> jdks =
            new com.intellij.openapi.ui.ComboBox<>();
    private final int requiredJava;
    private final boolean canApply;
    private final String report;
    private final List<MigrationWorkspace.Change> changes;
    private final Project project;
    private final JBCheckBox compile = new JBCheckBox(StudioBundle.message("checkbox.BuildTheProjectAfterApplying"), true);

    MigrationPreviewDialog(Project project, String title, String report, List<MigrationWorkspace.Change> changes, boolean canApply, int requiredJava) {
        super(project);
        this.project = project;
        this.requiredJava = requiredJava;
        this.canApply = canApply;
        for (var sdk : com.intellij.openapi.projectRoots.ProjectJdkTable.getInstance().getAllJdks()) {
            if (MigrationJdk.matches(sdk, requiredJava)) jdks.addItem(sdk);
        }
        jdks.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof com.intellij.openapi.projectRoots.Sdk sdk ? sdk.getName() : StudioBundle.message("label.NoMatchingJdkConfigured"),
                        index, selected, focus);
            }
        });
        this.report = report;
        this.changes = changes;
        setTitle(title);
        setOKButtonText(StudioBundle.message("button.ApplyMigration"));
        init();
        setOKActionEnabled(canApply && jdks.getItemCount() > 0);
        compile.setEnabled(canApply);
    }

    com.intellij.openapi.projectRoots.Sdk selectedJdk() {
        return (com.intellij.openapi.projectRoots.Sdk) jdks.getSelectedItem();
    }

    @Override protected com.intellij.openapi.ui.ValidationInfo doValidate() {
        if (canApply && !MigrationJdk.matches(selectedJdk(), requiredJava)) {
            return new com.intellij.openapi.ui.ValidationInfo(
                    StudioBundle.message("message.ConfigureJdkInProjectStructure", requiredJava), jdks);
        }
        return null;
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
        tabs.addTab(StudioBundle.message("tab.MigrationReport"), new JBScrollPane(summary));
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
                        change.before() == null ? StudioBundle.message("label.NewFile") : StudioBundle.message("label.Current"),
                        change.after() == null ? StudioBundle.message("label.DeleteFile") : StudioBundle.message("label.Proposed")));
            });
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JBScrollPane(paths), diff.getComponent());
            split.setDividerLocation(JBUI.scale(300));
            tabs.addTab(StudioBundle.message("tab.FileChangesCount", changed.size()), split);
            paths.setSelectedIndex(0);
        }
        panel.add(tabs, BorderLayout.CENTER);
        JPanel preparation = new JPanel();
        preparation.setLayout(new BoxLayout(preparation, BoxLayout.Y_AXIS));
        JBLabel label = new JBLabel(StudioBundle.message("label.TargetJdkJava", requiredJava));
        label.setLabelFor(jdks);
        preparation.add(label);
        preparation.add(jdks);
        preparation.add(new JBLabel(StudioBundle.message("label.MigrationPreparationNotice", requiredJava)));
        preparation.add(compile);
        if (canApply) panel.add(preparation, BorderLayout.SOUTH);
        return panel;
    }
}
