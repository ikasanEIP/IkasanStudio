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
import org.ikasan.studio.integration.ikasan.WiretapEventsClient;
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

/** Project-bound, read-only wiretap browser. HTTP work never runs on the EDT. */
// This dialog runs with project services in the local IDE; remote Split Mode needs a separate frontend and RPC boundary.
@SuppressWarnings("SplitModeApiUsage")
public final class WiretapEventsDialog extends DialogWrapper {
    private final Project project;
    private final URI endpoint;
    private final JBTextField component = new JBTextField(18);
    private final JBTextField flow = new JBTextField(18);
    private final JBTextField from = new JBTextField(19);
    private final JBTextField until = new JBTextField(19);
    private final JBLabel status = new JBLabel();
    private final JButton refresh = new JButton(StudioBundle.message("wiretapEvents.refresh"));
    private final JButton previous = new JButton(StudioBundle.message("wiretapEvents.previous"));
    private final JButton next = new JButton(StudioBundle.message("wiretapEvents.next"));
    private final JBTextArea details = new JBTextArea();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{StudioBundle.message("wiretapEvents.time"), StudioBundle.message("wiretapEvents.flow"),
            StudioBundle.message("wiretapEvents.component"), StudioBundle.message("wiretapEvents.eventId"), StudioBundle.message("wiretapEvents.harvested")}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JBTable table = new JBTable(model);
    private List<WiretapEventsClient.Event> rows = List.of();
    private int page;
    private boolean last = true;
    private Future<?> request;
    private long generation;
    private boolean closed;
    private String activeComponent = "";
    private String activeFlow = "", activeFrom = "", activeUntil = "";

    public static void open(Project project, String flowName, String componentName) {
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || !module.isInitialised()) return;
        try { new WiretapEventsDialog(project, ExcludedEventsClient.endpoint(module).resolve("../wiretap/"), flowName, componentName).show(); }
        catch (IllegalArgumentException e) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project, StudioBundle.message("wiretapEvents.configuration"), StudioBundle.message("wiretapEvents.title"));
        }
    }

    private WiretapEventsDialog(Project project, URI endpoint, String flowName, String componentName) {
        super(project, false);
        this.project = project;
        this.endpoint = endpoint;
        setTitle(StudioBundle.message("wiretapEvents.title"));
        setModal(false);
        setCancelButtonText(StudioBundle.message("wiretapEvents.close"));
        flow.setText(flowName == null ? "" : flowName);
        component.setText(componentName == null ? "" : componentName);
        init();
        refresh.addActionListener(e -> load(0, true));
        previous.addActionListener(e -> load(page - 1, false));
        next.addActionListener(e -> load(page + 1, false));
        component.addActionListener(e -> load(0, true));
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
        addField(flowRow, "wiretapEvents.flowFilter", flow); addField(flowRow, "wiretapEvents.component", component); flowRow.add(refresh);
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addField(dates, "wiretapEvents.from", from); addField(dates, "wiretapEvents.until", until);
        filters.add(new JBLabel(StudioBundle.message("wiretapEvents.description")));
        filters.add(new JBLabel(StudioBundle.message("wiretapEvents.captureHint")));
        filters.add(flowRow); filters.add(dates); filters.add(new JBLabel(StudioBundle.message("wiretapEvents.dateHint")));
        filters.add(new JBLabel(endpoint.toString()));
        panel.add(filters, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getEmptyText().setText(StudioBundle.message("wiretapEvents.empty"));
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.getAccessibleContext().setAccessibleName(StudioBundle.message("wiretapEvents.details"));
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("wiretapEvents.title"));
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
        String c = newSearch ? component.getText() : activeComponent;
        String f = newSearch ? flow.getText() : activeFlow;
        String start = newSearch ? from.getText() : activeFrom;
        String end = newSearch ? until.getText() : activeUntil;
        final URI uri;
        try { uri = WiretapEventsClient.query(endpoint, targetPage, f, c, start, end); }
        catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            status.setText(StudioBundle.message("wiretapEvents.invalidDates")); return;
        }
        long ticket = ++generation;
        if (request != null) request.cancel(true);
        busy(true); status.setText(StudioBundle.message("wiretapEvents.loading"));
        rows = List.of(); model.setRowCount(0); details.setText("");
        request = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            WiretapEventsClient.Page result = null;
            String failure = null;
            try { result = WiretapEventsClient.fetch(uri); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            catch (ExcludedEventsClient.HttpFailure e) {
                failure = e.status == 401 || e.status == 403 ? StudioBundle.message("wiretapEvents.authentication")
                        : e.status == 404 ? StudioBundle.message("wiretapEvents.notFound") : StudioBundle.message("wiretapEvents.http", e.status);
            } catch (java.net.ConnectException | java.net.http.HttpTimeoutException e) { failure = StudioBundle.message("wiretapEvents.connection"); }
            catch (Exception e) { failure = StudioBundle.message("wiretapEvents.failed"); }
            WiretapEventsClient.Page loaded = result;
            String error = failure;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (closed || project.isDisposed() || ticket != generation) return;
                if (error != null) { last = true; page = 0; busy(false); status.setText(error); return; }
                activeComponent = c; activeFlow = f; activeFrom = start; activeUntil = end;
                page = targetPage; last = loaded.last(); rows = loaded.events();
                for (var row : rows) model.addRow(new Object[]{time(row.timestamp()), row.flow(), row.component(), row.eventId(),
                        StudioBundle.message(row.harvested() ? "wiretapEvents.yes" : "wiretapEvents.no")});
                status.setText(StudioBundle.message("wiretapEvents.page", page + 1, loaded.total()));
                busy(false);
                if (!rows.isEmpty()) table.setRowSelectionInterval(0, 0);
            }, ModalityState.any());
        });
    }

    private void busy(boolean loading) {
        refresh.setEnabled(!loading); previous.setEnabled(!loading && page > 0); next.setEnabled(!loading && !last);
        component.setEnabled(!loading); flow.setEnabled(!loading); from.setEnabled(!loading); until.setEnabled(!loading);
    }

    private void showSelected() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) { details.setText(""); return; }
        var row = rows.get(index);
        details.setText(StudioBundle.message("wiretapEvents.module") + ": " + row.module() + "\n" + StudioBundle.message("wiretapEvents.flow") + ": " + row.flow()
                + "\n" + StudioBundle.message("wiretapEvents.identifier") + ": " + row.identifier() + "\n" + StudioBundle.message("wiretapEvents.component") + ": " + row.component()
                + "\n" + StudioBundle.message("wiretapEvents.eventId") + ": " + row.eventId() + "\n" + StudioBundle.message("wiretapEvents.relatedEventId") + ": " + row.relatedEventId()
                + "\n\n" + StudioBundle.message("wiretapEvents.payload") + "\n" + row.payload());
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
