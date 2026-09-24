package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.integration.sftp.RemoteFilesClient;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/** Session-only connection settings. All SSH and local file IO runs on a pooled thread. */
@SuppressWarnings("SplitModeApiUsage")
public final class SftpFilesDialog extends DialogWrapper {
    private final Project project;
    private final RemoteFilesClient.Connection connection;
    private final JBTextField directory = new JBTextField();
    private final JBTextArea status = new JBTextArea(2, 60);
    private final JButton go = new JButton(StudioBundle.message("sftpBrowser.go")), up = new JButton(StudioBundle.message("sftpBrowser.up")), refresh = new JButton(StudioBundle.message("sftpBrowser.refresh")),
            download = new JButton(StudioBundle.message("sftpBrowser.download")), delete = new JButton(StudioBundle.message("sftpBrowser.delete"));
    private final DefaultTableModel model = new DefaultTableModel(new String[]{StudioBundle.message("sftpBrowser.name"), StudioBundle.message("sftpBrowser.type"), StudioBundle.message("sftpBrowser.size"), StudioBundle.message("sftpBrowser.modified")}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JBTable table = new JBTable(model);
    private List<RemoteFilesClient.Entry> entries = List.of();
    private String listedDirectory;
    private boolean busy;
    private volatile boolean closed;
    private Future<?> request;
    private volatile RemoteFilesClient active;

    public static void open(Project project, FlowElement component) {
        Settings settings = new Settings(project, component);
        if (settings.showAndGet()) {
            SftpFilesDialog browser = new SftpFilesDialog(project, settings.connection(), settings.remote.getText().trim());
            browser.show();
            browser.load();
        }
    }
    private SftpFilesDialog(Project project, RemoteFilesClient.Connection connection, String initialDirectory) {
        super(project, false);
        this.project = project;
        this.connection = connection;
        directory.setText(initialDirectory);
        status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true); status.setOpaque(false);
        setTitle(StudioBundle.message("sftpBrowser.title") + " — " + connection.host + ":" + connection.port);
        setModal(false);
        setCancelButtonText(StudioBundle.message("sftpBrowser.close"));
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!busy && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        var entry = entries.get(table.convertRowIndexToModel(row));
                        if (entry.directory()) {
                            directory.setText(listedDirectory + (listedDirectory.endsWith("/") ? "" : "/") + entry.name());
                            load();
                        }
                    }
                }
            }
        });
        go.addActionListener(e -> load());
        directory.addActionListener(e -> { if (!busy) load(); });
        refresh.addActionListener(e -> load());
        up.addActionListener(e -> { directory.setText(listedDirectory + "/.."); load(); });
        download.addActionListener(e -> download());
        delete.addActionListener(e -> delete());
        init();
        updateButtons();
    }
    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel path = new JPanel(new BorderLayout(6, 0));
        path.add(directory, BorderLayout.CENTER);
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navigation.add(go); navigation.add(up); navigation.add(refresh);
        path.add(navigation, BorderLayout.EAST);
        panel.add(path, BorderLayout.NORTH);
        table.setPreferredScrollableViewportSize(new Dimension(760, 290));
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.add(download); actions.add(delete);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(new JBLabel(StudioBundle.message("sftpBrowser.historyNote")), BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }
    @Override protected Action @NotNull [] createActions() { return new Action[]{getCancelAction()}; }
    private List<RemoteFilesClient.Entry> selected() {
        return Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).mapToObj(entries::get).toList();
    }
    private void updateButtons() {
        var selected = selected();
        boolean files = !selected.isEmpty() && selected.stream().allMatch(RemoteFilesClient.Entry::regular);
        download.setEnabled(!busy && files && selected.size() == 1);
        delete.setEnabled(!busy && files);
        go.setEnabled(!busy); refresh.setEnabled(!busy); up.setEnabled(!busy && listedDirectory != null);
        directory.setEnabled(!busy); table.setEnabled(!busy);
    }
    private void load() {
        String target = directory.getText().trim();
        run(client -> new Result(client.list(target), null));
    }
    private void download() {
        var selection = selected();
        if (selection.size() != 1 || busy) return;
        String remote = listedDirectory;
        StudioProjectFiles.chooseFilePaths(project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), paths -> {
            if (closed || project.isDisposed() || busy || paths == null || paths.isEmpty()) return;
            Path destination = Path.of(paths.get(0));
            run(client -> {
                client.download(remote, selection.get(0), destination);
                return new Result(client.list(remote), StudioBundle.message("sftpBrowser.downloaded"));
            });
        });
    }
    private void delete() {
        var selection = selected();
        if (selection.isEmpty() || busy) return;
        String remote = listedDirectory;
        String names = String.join("\n", selection.stream().map(RemoteFilesClient.Entry::name).toList());
        if (Messages.showYesNoDialog(project, StudioBundle.message("sftpBrowser.confirmDelete", connection.host, connection.port, remote, names),
                StudioBundle.message("sftpBrowser.delete"), Messages.getWarningIcon()) != Messages.YES) return;
        run(client -> {
            int deleted = 0;
            try {
                for (var entry : selection) { client.delete(remote, entry); deleted++; }
            } catch (Exception failure) {
                // A batch can partially succeed. Report its exact count and refresh before another operation.
                return new Result(client.list(remote), StudioBundle.message("sftpBrowser.partialDelete", deleted, selection.size()));
            }
            return new Result(client.list(remote), StudioBundle.message("sftpBrowser.deleted", deleted));
        });
    }
    private record Result(RemoteFilesClient.Listing listing, String message) { }
    @FunctionalInterface private interface Work { Result execute(RemoteFilesClient client) throws Exception; }
    private void run(Work work) {
        if (busy || closed) return;
        busy = true; updateButtons(); status.setText(StudioBundle.message("sftpBrowser.working"));
        Path root = Path.of(project.getBasePath() == null ? "." : project.getBasePath());
        request = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Result result = null;
            String failure = null;
            boolean connected = false;
            try (RemoteFilesClient client = new RemoteFilesClient()) {
                active = client;
                if (closed || project.isDisposed()) return;
                client.connect(connection, root);
                connected = true;
                result = work.execute(client);
            } catch (Exception e) {
                // Do not expose credentials, key material or server-controlled exception text.
                failure = !connected ? StudioBundle.message("sftpBrowser.connectionFailed") :
                        e instanceof java.nio.file.FileAlreadyExistsException ? StudioBundle.message("sftpBrowser.exists") :
                        e instanceof RemoteFilesClient.ChangedFileException ? StudioBundle.message("sftpBrowser.changed") : StudioBundle.message("sftpBrowser.failed");
            } finally { active = null; }
            Result complete = result;
            String error = failure;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (closed || project.isDisposed()) return;
                busy = false;
                if (complete != null) {
                    table.clearSelection(); model.setRowCount(0);
                    entries = complete.listing().entries();
                    listedDirectory = complete.listing().directory(); directory.setText(listedDirectory);
                    for (var entry : entries) model.addRow(new Object[]{entry.name(),
                            entry.directory() ? StudioBundle.message("sftpBrowser.folder")
                                    : entry.regular() ? StudioBundle.message("sftpBrowser.file") : StudioBundle.message("sftpBrowser.other"), entry.size(), Instant.ofEpochMilli(entry.modified()).toString()});
                    status.setText(complete.message() != null ? complete.message() : (complete.listing().truncated()
                            ? StudioBundle.message("sftpBrowser.truncated", entries.size())
                            : StudioBundle.message("sftpBrowser.loaded", entries.size())));
                } else {
                    table.clearSelection(); model.setRowCount(0); entries = List.of(); listedDirectory = null;
                    status.setText(error);
                }
                updateButtons();
            }, ModalityState.any());
        });
    }
    @Override protected void dispose() {
        closed = true;
        if (request != null) request.cancel(true);
        RemoteFilesClient client = active;
        if (client != null) ApplicationManager.getApplication().executeOnPooledThread(client::close);
        super.dispose();
    }
    private static final class Settings extends DialogWrapper {
        private final JBTextField host = new JBTextField(), port = new JBTextField(), username = new JBTextField(),
                key = new JBTextField(), hosts = new JBTextField(), remote = new JBTextField();
        private final JPasswordField password = new JPasswordField(), passphrase = new JPasswordField();
        Settings(Project project, FlowElement component) {
            super(project, false);
            host.setText(value(component, "remoteHost")); port.setText(value(component, "remotePort"));
            if (port.getText().isBlank()) port.setText("22");
            username.setText(value(component, "username")); password.setText(value(component, "password"));
            key.setText(value(component, "privateKeyFilename")); hosts.setText(value(component, "knownHostFilename"));
            if (hosts.getText().isBlank()) hosts.setText("~/.ssh/known_hosts");
            remote.setText(value(component, component.getComponentMeta().isConsumer() ? "sourceDirectory" : "outputDirectory"));
            setTitle(StudioBundle.message("sftpBrowser.connectTitle")); setOKButtonText(StudioBundle.message("sftpBrowser.connect")); init();
        }
        private static String value(FlowElement component, String key) {
            String value = component.getPropertyValueAsString(key);
            if (value == null || value.isBlank()) {
                var meta = component.getComponentMeta().getMetadata(key);
                Object defaultValue = meta == null ? null : meta.getDefaultValue();
                value = defaultValue == null ? "" : defaultValue.toString();
            }
            return value;
        }
        RemoteFilesClient.Connection connection() {
            return new RemoteFilesClient.Connection(host.getText().trim(), Integer.parseInt(port.getText().trim()),
                    username.getText().trim(), new String(password.getPassword()), key.getText().trim(),
                    new String(passphrase.getPassword()), hosts.getText().trim());
        }
        @Override protected ValidationInfo doValidate() {
            if (host.getText().isBlank() || username.getText().isBlank() || hosts.getText().isBlank()
                    || (password.getPassword().length == 0 && key.getText().isBlank())) return new ValidationInfo(StudioBundle.message("sftpBrowser.required"));
            for (var field : List.of(host, port, username, password, key, hosts, remote))
                if (field.getText().contains("${")) return new ValidationInfo(StudioBundle.message("sftpBrowser.resolve"), field);
            try { connection(); } catch (IllegalArgumentException e) { return new ValidationInfo(StudioBundle.message("sftpBrowser.required")); }
            return null;
        }
        @Override protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            String[] labels = {
                    StudioBundle.message("sftpBrowser.host"), StudioBundle.message("sftpBrowser.port"),
                    StudioBundle.message("sftpBrowser.username"), StudioBundle.message("sftpBrowser.password"),
                    StudioBundle.message("sftpBrowser.key"), StudioBundle.message("sftpBrowser.passphrase"),
                    StudioBundle.message("sftpBrowser.hosts"), StudioBundle.message("sftpBrowser.directory")};
            JComponent[] fields = {host, port, username, password, key, passphrase, hosts, remote};
            for (int row = 0; row < fields.length; row++) {
                GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.insets = JBUI.insets(4); c.anchor = GridBagConstraints.WEST;
                JBLabel label = new JBLabel(labels[row]); label.setLabelFor(fields[row]); panel.add(label, c);
                c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; fields[row].setPreferredSize(new Dimension(390, 28)); panel.add(fields[row], c);
            }
            GridBagConstraints c = new GridBagConstraints(); c.gridy = fields.length; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(new JBLabel(StudioBundle.message("sftpBrowser.settingsNote")), c);
            return panel;
        }
    }
}
