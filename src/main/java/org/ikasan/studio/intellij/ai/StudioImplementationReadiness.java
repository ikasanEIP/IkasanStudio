package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.ui.JBUI;
import java.awt.Dimension;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.ikasan.studio.core.ai.ImplementationReadiness;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.*;

/** Saved-source report; all disk work is off EDT. The dialog is advisory and never changes user code. */
public final class StudioImplementationReadiness {
    private StudioImplementationReadiness() { }
    public static void check(Project project, boolean show) {
        String base = project.getBasePath();
        if (base == null || project.isDisposed()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path root = Path.of(base);
                var report = ImplementationReadiness.scanProject(root);
                Path destination = root.resolve("generated/implementation-readiness.json");
                Path temp = Files.createTempFile(destination.getParent(), ".readiness-", ".tmp");
                try {
                    Files.writeString(temp, StudioJson.newObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
                    try { Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                    catch (AtomicMoveNotSupportedException e) { Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING); }
                } finally { Files.deleteIfExists(temp); }
                if (show) ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) new Results(project, root, report).show();
                });
            } catch (Exception failure) {
                com.intellij.openapi.diagnostic.Logger.getInstance(StudioImplementationReadiness.class).warn("Implementation readiness check failed", failure);
                if (show) ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) com.intellij.openapi.ui.Messages.showWarningDialog(project,
                            StudioBundle.message("readiness.failed"), StudioBundle.message("readiness.title"));
                });
            }
        });
    }
    // Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
    @SuppressWarnings("SplitModeApiUsage")
    private static final class Results extends DialogWrapper {
        private final Project project;
        private final Path root;
        private final ImplementationReadiness.Report report;
        private final JBTable table;
        private final int contentWidth;
        private final int heightLimit;
        Results(Project project, Path root, ImplementationReadiness.Report report) {
            super(project, false); this.project = project; this.root = root; this.report = report;
            var frame = WindowManager.getInstance().getFrame(project);
            var available = frame == null ? java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds().getSize() : frame.getSize();
            contentWidth = Math.min(JBUI.scale(900), Math.max(1, available.width - JBUI.scale(100)));
            heightLimit = Math.max(1, available.height / 2);
            var model = new DefaultTableModel(new String[]{StudioBundle.message("readiness.flow"), StudioBundle.message("readiness.component"), StudioBundle.message("readiness.finding"), StudioBundle.message("readiness.file")}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (var f : report.findings()) model.addRow(new Object[]{f.flow(), f.component(), f.code() + ": " + f.message(), f.file() + ":" + f.line()});
            table = new JBTable(model); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getEmptyText().setText(StudioBundle.message("readiness.empty"));
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) return;
                    int row = table.rowAtPoint(event.getPoint());
                    if (row < 0) return;
                    table.setRowSelectionInterval(row, row);
                    openSelectedSource();
                }
            });
            setTitle(StudioBundle.message("readiness.title")); setModal(false); setCancelButtonText(StudioBundle.message("readiness.close")); init();
            pack();
            // Include the title, description and action buttons in the half-window limit.
            var size = getSize();
            setSize(size.width, Math.min(size.height, heightLimit));
        }
        @Override protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new java.awt.BorderLayout(0, JBUI.scale(8)));
            var description = new StudioAiConnectionText(StudioBundle.message("readiness.scope"));
            description.setEditable(false); description.setLineWrap(true); description.setWrapStyleWord(true);
            description.setOpaque(false);
            description.setFont(UIManager.getFont("Label.font"));
            description.setSize(contentWidth, 1);
            // Fit the actual rows, leaving enough space for the empty-state message.
            int rowsHeight = (int) Math.min((long) Math.max(2, table.getRowCount()) * table.getRowHeight(), heightLimit);
            table.setPreferredScrollableViewportSize(new Dimension(contentWidth, rowsHeight));
            panel.add(description, java.awt.BorderLayout.NORTH);
            panel.add(new JBScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), java.awt.BorderLayout.CENTER);
            return panel;
        }
        private void openSelectedSource() {
            int row = table.getSelectedRow();
            if (row < 0 || project.isDisposed()) return;
            Path file = root.resolve(report.findings().get(table.convertRowIndexToModel(row)).file());
            ApplicationManager.getApplication().executeOnPooledThread(() -> StudioNavigator.openFileByNioPath(project, file));
        }

        @Override protected Action @org.jetbrains.annotations.NotNull [] createActions() {
            return new Action[]{new AbstractAction(StudioBundle.message("readiness.open")) {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                    openSelectedSource();
                }
            }, new AbstractAction(StudioBundle.message("readiness.refresh")) {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { close(CANCEL_EXIT_CODE); check(project, true); }
            }, getCancelAction()};
        }
    }
}
