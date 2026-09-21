package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.ExcludedEventsClient;
import org.ikasan.studio.integration.ikasan.FileDuplicateHistoryClient;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Future;

/** Project-bound, read-only file duplicate history browser. HTTP work never runs on the EDT. */
// This dialog runs with project services in the local IDE; remote Split Mode needs a separate frontend and RPC boundary.
@SuppressWarnings("SplitModeApiUsage")
public final class FileDuplicateHistoryDialog extends DialogWrapper {
    private final Project project;
    private final URI endpoint;
    private final JBTextField clientId = new JBTextField(18);
    private final JBTextField criteria = new JBTextField(19);
    private final JBLabel status = new JBLabel();
    private final JButton refresh = new JButton(StudioBundle.message("fileHistory.refresh"));
    private final JButton previous = new JButton(StudioBundle.message("fileHistory.previous"));
    private final JButton next = new JButton(StudioBundle.message("fileHistory.next"));
    private final JBTextArea details = new JBTextArea();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{StudioBundle.message("fileHistory.criteria"), StudioBundle.message("fileHistory.clientId"), StudioBundle.message("fileHistory.size"), StudioBundle.message("fileHistory.modified"), StudioBundle.message("fileHistory.created")}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JBTable table = new JBTable(model);
    private List<FileDuplicateHistoryClient.Entry> rows = List.of();
    private int page;
    private boolean last = true;
    private Future<?> request;
    private long generation;
    private boolean closed;
    private String activeClient = "", activeCriteria = "";

    public static void open(Project project) {
        open(project, "");
    }

    public static void open(Project project, String initialClientId) {
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || !module.isInitialised()) return;
        try { new FileDuplicateHistoryDialog(project, ExcludedEventsClient.endpoint(module).resolve("../filefilter/search"), initialClientId).show(); }
        catch (IllegalArgumentException e) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project, StudioBundle.message("fileHistory.configuration"), StudioBundle.message("fileHistory.title"));
        }
    }

    private FileDuplicateHistoryDialog(Project project, URI endpoint, String initialClientId) {
        super(project, false);
        this.project = project;
        this.endpoint = endpoint;
        clientId.setText(initialClientId == null ? "" : initialClientId);
        setTitle(StudioBundle.message("fileHistory.title"));
        setModal(false);
        setCancelButtonText(StudioBundle.message("fileHistory.close"));
        init();
        refresh.addActionListener(e -> load(0, true));
        previous.addActionListener(e -> load(page - 1, false));
        next.addActionListener(e -> load(page + 1, false));
        clientId.addActionListener(e -> load(0, true));
        criteria.addActionListener(e -> load(0, true));
        table.getSelectionModel().addListSelectionListener(e -> showSelected());
        load(0, true);
    }

    @Override protected Action @org.jetbrains.annotations.NotNull [] createActions() { return new Action[]{getCancelAction()}; }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel filters = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        JPanel flowRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addField(flowRow, "fileHistory.clientId", clientId); flowRow.add(refresh);
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addField(dates, "fileHistory.criteria", criteria);
        filters.add(new JBLabel(StudioBundle.message("fileHistory.description")));
        filters.add(flowRow); filters.add(dates); filters.add(new JBLabel(StudioBundle.message("fileHistory.filterHint")));
        filters.add(new JBLabel(endpoint.toString()));
        panel.add(filters, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getEmptyText().setText(StudioBundle.message("fileHistory.empty"));
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.getAccessibleContext().setAccessibleName(StudioBundle.message("fileHistory.details"));
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("fileHistory.title"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JBScrollPane(table), new JBScrollPane(details));
        split.setResizeWeight(0.55); split.setDividerLocation(JBUI.scale(240));
        panel.add(split, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttons.add(previous); buttons.add(next);
        footer.add(buttons, BorderLayout.WEST); footer.add(status, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        panel.setPreferredSize(JBUI.size(850, 560));
        return panel;
    }

    private static void addField(JPanel panel, @org.jetbrains.annotations.PropertyKey(resourceBundle = "messages.studioBundle") String key, JBTextField field) {
        JBLabel label = new JBLabel(StudioBundle.message(key)); label.setLabelFor(field); panel.add(label); panel.add(field);
    }

    private void load(int targetPage, boolean newSearch) {
        String f = newSearch ? clientId.getText() : activeClient;
        String start = newSearch ? criteria.getText() : activeCriteria;
        final URI uri;
        try { uri = FileDuplicateHistoryClient.query(endpoint, targetPage, f, start); }
        catch (IllegalArgumentException e) {
            status.setText(StudioBundle.message("fileHistory.failed")); return;
        }
        long ticket = ++generation;
        if (request != null) request.cancel(true);
        busy(true); status.setText(StudioBundle.message("fileHistory.loading"));
        rows = List.of(); model.setRowCount(0); details.setText("");
        request = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            FileDuplicateHistoryClient.Page result = null;
            String failure = null;
            try { result = FileDuplicateHistoryClient.fetch(uri); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            catch (ExcludedEventsClient.HttpFailure e) {
                failure = e.status == 401 || e.status == 403 ? StudioBundle.message("fileHistory.authentication")
                        : e.status == 404 ? StudioBundle.message("fileHistory.notFound") : StudioBundle.message("fileHistory.http", e.status);
            } catch (java.net.ConnectException | java.net.http.HttpTimeoutException e) { failure = StudioBundle.message("fileHistory.connection"); }
            catch (Exception e) { failure = StudioBundle.message("fileHistory.failed"); }
            FileDuplicateHistoryClient.Page loaded = result;
            String error = failure;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (closed || project.isDisposed() || ticket != generation) return;
                if (error != null) { last = true; page = 0; busy(false); status.setText(error); return; }
                activeClient = f; activeCriteria = start;
                page = targetPage; last = loaded.last(); rows = loaded.entries();
                for (var row : rows) model.addRow(new Object[]{row.criteria(), row.clientId(), row.size(), row.modified(), row.created()});
                status.setText(StudioBundle.message("fileHistory.page", page + 1, loaded.total()));
                busy(false);
                if (!rows.isEmpty()) table.setRowSelectionInterval(0, 0);
            }, ModalityState.any());
        });
    }

    private void busy(boolean loading) {
        refresh.setEnabled(!loading); previous.setEnabled(!loading && page > 0); next.setEnabled(!loading && !last);
        clientId.setEnabled(!loading); criteria.setEnabled(!loading);
    }

    private void showSelected() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) { details.setText(""); return; }
        var row = rows.get(index);
        details.setText(StudioBundle.message("fileHistory.id") + ": " + row.id() + "\n" + StudioBundle.message("fileHistory.clientId") + ": " + row.clientId()
                + "\n" + StudioBundle.message("fileHistory.criteria") + ": " + row.criteria() + "\n" + StudioBundle.message("fileHistory.size") + ": " + row.size()
                + "\n" + StudioBundle.message("fileHistory.modified") + ": " + row.modified() + "\n" + StudioBundle.message("fileHistory.accessed") + ": " + row.accessed()
                + "\n" + StudioBundle.message("fileHistory.created") + ": " + row.created() + "\n\n" + StudioBundle.message("fileHistory.explanation"));
        details.setCaretPosition(0);
    }

    @Override protected void dispose() {
        closed = true; generation++;
        if (request != null) request.cancel(true);
        super.dispose();
    }
}
