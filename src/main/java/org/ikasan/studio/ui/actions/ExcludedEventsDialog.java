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
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Future;

/** Project-bound, read-only exclusion browser. HTTP work never runs on the EDT. */
// This dialog runs with project services in the local IDE; remote Split Mode needs a separate frontend and RPC boundary.
@SuppressWarnings("SplitModeApiUsage")
public final class ExcludedEventsDialog extends DialogWrapper {
    private final Project project;
    private final URI endpoint;
    private final JBTextField flow = new JBTextField(18);
    private final JBTextField from = new JBTextField(19);
    private final JBTextField until = new JBTextField(19);
    private final JBLabel status = new JBLabel();
    private final JButton refresh = new JButton(StudioBundle.message("exclusions.refresh"));
    private final JButton previous = new JButton(StudioBundle.message("exclusions.previous"));
    private final JButton next = new JButton(StudioBundle.message("exclusions.next"));
    private final JBTextArea details = new JBTextArea();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{StudioBundle.message("exclusions.time"), StudioBundle.message("exclusions.flow"),
            StudioBundle.message("exclusions.identifier"), StudioBundle.message("exclusions.harvested")}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JBTable table = new JBTable(model);
    private List<ExcludedEventsClient.Event> rows = List.of();
    private int page;
    private boolean last = true;
    private Future<?> request;
    private long generation;
    private boolean closed;
    private String activeFlow = "", activeFrom = "", activeUntil = "";

    public static void open(Project project, String flowName) {
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || !module.isInitialised()) return;
        try { new ExcludedEventsDialog(project, ExcludedEventsClient.endpoint(module), flowName).show(); }
        catch (IllegalArgumentException e) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project, StudioBundle.message("exclusions.configuration"), StudioBundle.message("exclusions.title"));
        }
    }

    private ExcludedEventsDialog(Project project, URI endpoint, String flowName) {
        super(project, false);
        this.project = project;
        this.endpoint = endpoint;
        setTitle(StudioBundle.message("exclusions.title"));
        setModal(false);
        setCancelButtonText(StudioBundle.message("exclusions.close"));
        flow.setText(flowName == null ? "" : flowName);
        init();
        refresh.addActionListener(e -> load(0, true));
        previous.addActionListener(e -> load(page - 1, false));
        next.addActionListener(e -> load(page + 1, false));
        flow.addActionListener(e -> load(0, true));
        from.addActionListener(e -> load(0, true));
        until.addActionListener(e -> load(0, true));
        table.getSelectionModel().addListSelectionListener(e -> showSelected());
        load(0, true);
    }

    @Override protected Action @org.jetbrains.annotations.NotNull [] createActions() { return new Action[]{getCancelAction()}; }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel filters = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        JPanel flowRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addField(flowRow, "exclusions.flowFilter", flow); flowRow.add(refresh);
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addField(dates, "exclusions.from", from); addField(dates, "exclusions.until", until);
        filters.add(new JBLabel(StudioBundle.message("exclusions.description")));
        filters.add(flowRow); filters.add(dates); filters.add(new JBLabel(StudioBundle.message("exclusions.dateHint")));
        filters.add(new JBLabel(endpoint.toString()));
        panel.add(filters, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getEmptyText().setText(StudioBundle.message("exclusions.empty"));
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.getAccessibleContext().setAccessibleName(StudioBundle.message("exclusions.details"));
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("exclusions.title"));
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
        String f = newSearch ? flow.getText() : activeFlow;
        String start = newSearch ? from.getText() : activeFrom;
        String end = newSearch ? until.getText() : activeUntil;
        final URI uri;
        try { uri = ExcludedEventsClient.query(endpoint, targetPage, f, start, end); }
        catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            status.setText(StudioBundle.message("exclusions.invalidDates")); return;
        }
        long ticket = ++generation;
        if (request != null) request.cancel(true);
        busy(true); status.setText(StudioBundle.message("exclusions.loading"));
        rows = List.of(); model.setRowCount(0); details.setText("");
        request = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ExcludedEventsClient.Page result = null;
            String failure = null;
            try { result = ExcludedEventsClient.fetch(uri); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            catch (ExcludedEventsClient.HttpFailure e) {
                failure = e.status == 401 || e.status == 403 ? StudioBundle.message("exclusions.authentication")
                        : e.status == 404 ? StudioBundle.message("exclusions.notFound") : StudioBundle.message("exclusions.http", e.status);
            } catch (java.net.ConnectException | java.net.http.HttpTimeoutException e) { failure = StudioBundle.message("exclusions.connection"); }
            catch (Exception e) { failure = StudioBundle.message("exclusions.failed"); }
            ExcludedEventsClient.Page loaded = result;
            String error = failure;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (closed || project.isDisposed() || ticket != generation) return;
                if (error != null) { last = true; page = 0; busy(false); status.setText(error); return; }
                activeFlow = f; activeFrom = start; activeUntil = end;
                page = targetPage; last = loaded.last(); rows = loaded.events();
                for (var row : rows) model.addRow(new Object[]{time(row.timestamp()), row.flow(), row.identifier(),
                        StudioBundle.message(row.harvested() ? "exclusions.yes" : "exclusions.no")});
                status.setText(StudioBundle.message("exclusions.page", page + 1, loaded.total()));
                busy(false);
                if (!rows.isEmpty()) table.setRowSelectionInterval(0, 0);
            }, ModalityState.any());
        });
    }

    private void busy(boolean loading) {
        refresh.setEnabled(!loading); previous.setEnabled(!loading && page > 0); next.setEnabled(!loading && !last);
        flow.setEnabled(!loading); from.setEnabled(!loading); until.setEnabled(!loading);
    }

    private void showSelected() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) { details.setText(""); return; }
        var row = rows.get(index);
        details.setText(StudioBundle.message("exclusions.module") + ": " + row.module() + "\n" + StudioBundle.message("exclusions.flow") + ": " + row.flow()
                + "\n" + StudioBundle.message("exclusions.identifier") + ": " + row.identifier() + "\n" + StudioBundle.message("exclusions.errorUri") + ": " + row.errorUri()
                + "\n\n" + StudioBundle.message(row.binary() ? "exclusions.binary" : "exclusions.payload") + "\n" + row.payload());
        details.setCaretPosition(0);
    }

    private static String time(long millis) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }
    @Override protected void dispose() {
        closed = true; generation++;
        if (request != null) request.cancel(true);
        super.dispose();
    }
}
