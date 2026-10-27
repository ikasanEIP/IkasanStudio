package org.ikasan.studio.intellij.project;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.integration.ikasan.RuntimeImportClient;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/** Fetches into memory only. The parent import dialog still owns conversion, review and import. */
final class RuntimeImportConnectionDialog extends ImportDialogWrapper {
    private static final com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(RuntimeImportConnectionDialog.class);
    private final Project project;
    private final JBTextField url = new JBTextField("http://localhost:8080/");
    private final JBTextField module = new JBTextField();
    private final JBTextField username = new JBTextField();
    private final JBPasswordField password = new JBPasswordField();
    private final JBCheckBox configuration = new JBCheckBox(StudioBundle.message("import.remote.includeConfiguration"), true);
    private RuntimeImportClient.Documents documents;

    RuntimeImportConnectionDialog(Project project) {
        this(project, null);
    }

    RuntimeImportConnectionDialog(Project project, Component parent) {
        super(project, parent);
        this.project = project;
        var saved = project.getService(RuntimeImportConnectionSettings.class).getState();
        url.setText(saved.applicationUrl);
        module.setText(saved.moduleName);
        username.setText(saved.username);
        configuration.setSelected(saved.includeConfiguration);
        setTitle(StudioBundle.message("import.remote.title"));
        setOKButtonText(StudioBundle.message("import.remote.fetch"));
        init();
    }

    @Override protected JComponent createCenterPanel() {
        var panel = new JPanel(new BorderLayout(0, JBUI.scale(10)));
        var note = new JBTextArea(StudioBundle.message("import.remote.help"), 6, 60);
        note.setEditable(false); note.setLineWrap(true); note.setWrapStyleWord(true);
        note.setOpaque(false); note.setFont(UIManager.getFont("Label.font"));
        panel.add(note, BorderLayout.NORTH);
        var fields = new JPanel(new GridBagLayout());
        add(fields, 0, "import.remote.url", url);
        add(fields, 1, "import.remote.module", module);
        add(fields, 2, "import.remote.username", username);
        add(fields, 3, "import.remote.password", password);
        panel.add(fields, BorderLayout.CENTER);
        var bottom = new JPanel(new BorderLayout());
        bottom.add(configuration, BorderLayout.CENTER);
        var reportButton = new JButton(StudioBundle.message("import.diagnostics.title"));
        reportButton.addActionListener(event -> new RuntimeImportReportDialog(project, getContentPane()).show());
        bottom.add(reportButton, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private static void add(JPanel panel, int row, String key, JComponent input) {
        var label = new JBLabel(StudioBundle.message(key)); label.setLabelFor(input);
        var c = new GridBagConstraints(); c.gridx = 0; c.gridy = row;
        c.anchor = GridBagConstraints.WEST; c.insets = JBUI.insets(4, 0, 4, 10);
        panel.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, c);
    }

    @Override public JComponent getPreferredFocusedComponent() { return url; }

    @Override protected ValidationInfo doValidate() {
        try { RuntimeImportClient.baseUri(url.getText()); }
        catch (IllegalArgumentException invalid) { return new ValidationInfo(StudioBundle.message("import.remote.urlInvalid"), url); }
        if (module.getText().isBlank()) return new ValidationInfo(StudioBundle.message("import.remote.moduleRequired"), module);
        if (username.getText().contains(":")) return new ValidationInfo(StudioBundle.message("import.remote.usernameInvalid"), username);
        char[] secret = password.getPassword();
        try {
            if (username.getText().isBlank() && secret.length > 0)
                return new ValidationInfo(StudioBundle.message("import.remote.usernameRequired"), username);
        } finally { java.util.Arrays.fill(secret, '\0'); }
        return null;
    }

    @Override protected void doOKAction() {
        if (doValidate() != null) { initValidation(); return; }
        var base = RuntimeImportClient.baseUri(url.getText());
        url.setText(base.toString());
        String name = module.getText().trim();
        String user = username.getText();
        char[] secret = password.getPassword();
        boolean include = configuration.isSelected();
        rememberConnection();
        var report = org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics.get(project).start();
        report.stage(org.ikasan.studio.core.diagnostics.RuntimeImportReport.Stage.FETCH);
        var fetched = new AtomicReference<RuntimeImportClient.Documents>();
        var failure = new AtomicReference<IOException>();
        try {
            boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    fetched.set(RuntimeImportClient.fetch(base, name, user, secret, include,
                            () -> { ProgressManager.checkCanceled(); if (project.isDisposed()) throw new ProcessCanceledException(); }, LOG::info, (stage, status) -> {
                                report.request(stage == RuntimeImportClient.RequestStage.METADATA
                                        ? org.ikasan.studio.core.diagnostics.RuntimeImportReport.Endpoint.METADATA
                                        : org.ikasan.studio.core.diagnostics.RuntimeImportReport.Endpoint.CONFIGURATION, status);
                                if (status > 0 && status != 200) report.issue("DOCUMENT", stage.name() + "_HTTP_" + status);
                            }));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); throw new ProcessCanceledException();
                } catch (IOException problem) { failure.set(problem); }
            }, StudioBundle.message("import.remote.progress"), true, project);
            if (!completed || project.isDisposed() || isDisposed()) { report.cancelled(); return; }
            if (failure.get() != null) {
                report.failure(failure.get());
                setErrorText(StudioBundle.message("import.remote.failed", failure.get().getMessage()));
                return;
            }
            report.complete();
            documents = fetched.get();
            if (documents != null) close(OK_EXIT_CODE);
        } catch (ProcessCanceledException cancelled) {
            report.cancelled(); throw cancelled;
        } finally { java.util.Arrays.fill(secret, '\0'); }
    }

    private void rememberConnection() {
        if (!project.isDisposed()) {
            project.getService(RuntimeImportConnectionSettings.class).remember(
                    url.getText(), module.getText(), username.getText(), configuration.isSelected());
        }
    }

    @Override protected void dispose() {
        rememberConnection();
        password.setText("");
        super.dispose();
    }

    RuntimeImportClient.Documents documents() { return documents; }
}
