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
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.editor.IkasanStudioEditorService;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.*;

/** Saved-model and source report; all disk work is off EDT. The dialog is advisory and never changes user code. */
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
    static BasicElement resolveTarget(org.ikasan.studio.core.model.ikasan.instance.Module module,
                                      ImplementationReadiness.Finding finding) {
        if (module == null || finding.target() == null) return null;
        if (finding.target() == ImplementationReadiness.Target.MODULE) return module;
        for (var flow : module.getFlows()) {
            if (!java.util.Objects.equals(flow.getIdentity(), finding.flow())) continue;
            if (finding.target() == ImplementationReadiness.Target.FLOW) return flow;
            if (finding.target() == ImplementationReadiness.Target.EXCEPTION_RESOLVER) return flow.getExceptionResolver();
            for (var element : flow.ftlGetConsumerAndFlowElements())
                if (java.util.Objects.equals(element.getIdentity(), finding.component())) return element;
        }
        return null;
    }

    // Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
    @SuppressWarnings("SplitModeApiUsage")
    private static final class Results extends DialogWrapper {
        private final Project project;
        private final Path root;
        private final ImplementationReadiness.Report report;
        private final JBTable table;
        private final JTextArea details = new JTextArea(5, 60);
        private Action navigateAction;
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
            for (var f : report.findings()) model.addRow(new Object[]{f.flow(), f.component(), f.code() + ": " + f.message(), isModelFinding(f) ? StudioBundle.message("readiness.properties") : f.file() + ":" + f.line()});
            table = new JBTable(model); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(event -> updateSelection());
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
            if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
            updateSelection();
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
            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            details.setFont(UIManager.getFont("Label.font"));
            details.getAccessibleContext().setAccessibleName(StudioBundle.message("readiness.details"));
            panel.add(new JBScrollPane(details), java.awt.BorderLayout.SOUTH);
            return panel;
        }
        private static boolean isModelFinding(ImplementationReadiness.Finding finding) {
            return "generated/src/main/model/model.json".equals(finding.file());
        }

        private ImplementationReadiness.Finding selectedFinding() {
            int row = table.getSelectedRow();
            return row < 0 ? null : report.findings().get(table.convertRowIndexToModel(row));
        }

        private void updateSelection() {
            var finding = selectedFinding();
            if (navigateAction != null) {
                navigateAction.putValue(Action.NAME, StudioBundle.message(finding != null && isModelFinding(finding)
                        ? "readiness.showStudio" : "readiness.open"));
                navigateAction.setEnabled(finding != null && (!isModelFinding(finding) || finding.target() != null));
            }
            details.setText(finding == null ? "" : finding.flow() + " / " + finding.component() + "\n"
                    + finding.message() + (finding.property() == null ? "" : "\n"
                    + StudioBundle.message("readiness.value", finding.property(), finding.displayValue())));
            details.setCaretPosition(0);
        }

        private void openSelectedSource() {
            var finding = selectedFinding();
            if (finding == null || project.isDisposed()) return;
            if (isModelFinding(finding)) {
                if (finding.target() == null) return;
                project.getService(IkasanStudioEditorService.class).open();
                UiContext context = project.getService(UiContext.class);
                BasicElement target = resolveTarget(context.getIkasanModule(), finding);
                if (target == null || context.getDesignerCanvas() == null || context.getDesignerUI() == null) {
                    com.intellij.openapi.ui.Messages.showInfoMessage(project,
                            StudioBundle.message("readiness.targetUnavailable"), StudioBundle.message("readiness.title"));
                    return;
                }
                context.getDesignerCanvas().editComponent(target);
                if (context.getSelectedComponent() != target) return; // Pending-edit selection change was cancelled.
                close(CANCEL_EXIT_CODE);
                context.getDesignerUI().showProperties();
                if (finding.target() != ImplementationReadiness.Target.EXCEPTION_RESOLVER
                        && context.getPropertiesPanel() != null && finding.property() != null)
                    context.getPropertiesPanel().revealProperty(finding.property());
                return;
            }
            Path file = root.resolve(finding.file()).normalize();
            if (!file.startsWith(root.normalize())) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> StudioNavigator.openFileByNioPath(project, file, finding.line()));
        }

        @Override protected Action @org.jetbrains.annotations.NotNull [] createActions() {
            navigateAction = new AbstractAction(StudioBundle.message("readiness.open")) {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                    openSelectedSource();
                }
            };
            return new Action[]{navigateAction, new AbstractAction(StudioBundle.message("readiness.refresh")) {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { close(CANCEL_EXIT_CODE); check(project, true); }
            }, getCancelAction()};
        }
    }
}
