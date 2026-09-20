package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.Disposer;

public final class StudioAiConnectionAction extends DumbAwareAction {
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(@NotNull AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null); }
    @Override public void actionPerformed(@NotNull AnActionEvent event) { if (event.getProject() != null) open(event.getProject()); }
    public static void open(Project project) {
        new Task.Backgroundable(project, StudioBundle.message("ai.ConnectionTitle"), false) {
            @Override public void run(@NotNull ProgressIndicator indicator) {
                try {
                    var service = project.getService(StudioAiService.class);
                    String configuration = service.start();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed() && service.isRunning()) new ConnectionDialog(project, service, configuration).show();
                    });
                } catch (Exception failure) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("ai.ConnectionFailed", failure.getMessage()));
                    });
                }
            }
        }.queue();
    }
    // This dialog and its project service currently run together in the local IDE.
    // Remote-development support requires a frontend UI and an RPC boundary to the service;
    // the optional native MCP content module does not provide that UI split.
    @SuppressWarnings("SplitModeApiUsage")
    private static final class ConnectionDialog extends DialogWrapper {
        private final StudioAiService service;
        private final String configuration;
        private final Project project;
        private boolean promptCopied;
        private Runnable refreshStatus = () -> { };
        ConnectionDialog(Project project, StudioAiService service, String configuration) {
            super(project, false); this.project = project; this.service = service; this.configuration = configuration;
            setModal(false);
            setTitle(StudioBundle.message("ai.ConnectionTitle"));
            setOKButtonText(StudioBundle.message("button.Close"));
            init();
        }
        @Override protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
            panel.add(text(StudioBundle.message("ai.ConnectionExplanation")), BorderLayout.NORTH);
            JTabbedPane tabs = new com.intellij.ui.components.JBTabbedPane();
            boolean nativeAvailable = StudioNativeMcpSupport.available();
            if (nativeAvailable) {
                JPanel nativePanel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
                nativePanel.add(new StudioAiConnectionInstructions(() -> StudioMcpSettings.open(project)),
                        BorderLayout.CENTER);
                tabs.addTab(StudioBundle.message("ai.NativeTab"), nativePanel);
            }
            JPanel manual = new JPanel(new BorderLayout(0, JBUI.scale(8)));
            manual.add(new StudioAiConnectionInstructions(StudioBundle.message("ai.AdapterInstructions"),
                    StudioBundle.message("ai.AdapterTab")), BorderLayout.NORTH);
            JBTextArea config = text(configuration);
            config.setLineWrap(false);
            config.setRows(6);
            manual.add(new JBScrollPane(config), BorderLayout.CENTER);
            JButton copy = new JButton(StudioBundle.message("ai.CopyConfiguration"));
            copy.addActionListener(event -> CopyPasteManager.getInstance().setContents(new StringSelection(configuration)));
            manual.add(copy, BorderLayout.SOUTH);
            tabs.addTab(StudioBundle.message("ai.AdapterTab"), manual);
            JPanel fileSetup = new JPanel(new BorderLayout(0, JBUI.scale(8)));
            fileSetup.add(new StudioAiConnectionInstructions(StudioBundle.message("ai.FileInstructions"),
                    StudioBundle.message("ai.FileTab")), BorderLayout.CENTER);
            JButton importFile = new JButton(StudioBundle.message("ai.ImportTitle"));
            importFile.addActionListener(event -> StudioAiImportProposalAction.open(project));
            fileSetup.add(importFile, BorderLayout.SOUTH);
            tabs.addTab(StudioBundle.message("ai.FileTab"), fileSetup);
            panel.add(tabs, BorderLayout.CENTER);
            JPanel footer = new JPanel(new BorderLayout(0, JBUI.scale(8)));
            var reconnect = new com.intellij.ui.components.JBCheckBox(StudioBundle.message("ai.Reconnect"),
                    PropertiesComponent.getInstance(project).getBoolean(StudioAiStartupActivity.RECONNECT, true));
            PropertiesComponent.getInstance(project).setValue(StudioAiStartupActivity.RECONNECT,
                    Boolean.toString(reconnect.isSelected()));
            reconnect.addActionListener(event -> PropertiesComponent.getInstance(project)
                    .setValue(StudioAiStartupActivity.RECONNECT, Boolean.toString(reconnect.isSelected())));
            footer.add(reconnect, BorderLayout.NORTH);
            JPanel checklist = new JPanel(new java.awt.GridBagLayout());
            checklist.setBorder(javax.swing.BorderFactory.createTitledBorder(StudioBundle.message("ai.ChecklistTitle")));
            var access = new com.intellij.ui.components.JBLabel();
            var model = new com.intellij.ui.components.JBLabel();
            var route = new com.intellij.ui.components.JBLabel();
            var client = new com.intellij.ui.components.JBLabel();
            var routeLabel = new com.intellij.ui.components.JBLabel();
            javax.swing.JLabel[] labels = {
                    new com.intellij.ui.components.JBLabel(StudioBundle.message("ai.CheckAccessLabel")),
                    new com.intellij.ui.components.JBLabel(StudioBundle.message("ai.CheckModelLabel")),
                    routeLabel,
                    new com.intellij.ui.components.JBLabel(StudioBundle.message("ai.CheckClientLabel"))};
            javax.swing.JLabel[] values = {access, model, route, client};
            for (int row = 0; row < labels.length; row++) {
                var constraints = new java.awt.GridBagConstraints();
                constraints.gridy = row;
                constraints.gridx = 0;
                constraints.anchor = java.awt.GridBagConstraints.LINE_START;
                constraints.insets = JBUI.insets(2, 0, 2, 12);
                labels[row].setLabelFor(values[row]);
                checklist.add(labels[row], constraints);
                constraints.gridx = 1;
                constraints.weightx = 1;
                constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                constraints.insets = JBUI.insets(2, 0);
                checklist.add(values[row], constraints);
            }
            JPanel guidance = new JPanel(new BorderLayout(0, JBUI.scale(8)));
            guidance.add(checklist, BorderLayout.NORTH);
            JBTextArea next = text("");
            next.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.NextStepTitle"));
            guidance.add(next, BorderLayout.CENTER);
            refreshStatus = () -> {
                if (project.isDisposed()) return;
                boolean fileRoute = tabs.getSelectedIndex() == tabs.getTabCount() - 1;
                guidance.setVisible(!fileRoute);
                reconnect.setVisible(!fileRoute);
                boolean running = service.isRunning();
                String issue = running ? service.connectionReadinessIssue() : null;
                String transport = service.getLastAccessTransport();
                boolean nativeRoute = nativeAvailable && tabs.getSelectedIndex() == 0;
                access.setText(StudioBundle.message(running ? "ai.CheckAccessReady" : "ai.StatusStopped"));
                model.setText(StudioBundle.message(!running ? "ai.CheckModelStopped"
                        : issue == null ? "ai.CheckModelReady" : "ai.CheckModelWaiting"));
                model.setToolTipText(issue);
                routeLabel.setText(StudioBundle.message(nativeRoute ? "ai.CheckNativeLabel" : "ai.CheckAdapterLabel"));
                route.setText(StudioBundle.message(nativeRoute ? "ai.CheckNativeAvailable" : "ai.CheckAdapterAvailable"));
                client.setText(StudioBundle.message("native".equals(transport) ? "ai.StatusNative"
                        : "adapter".equals(transport) ? "ai.StatusAdapter" : "ai.CheckClientWaiting"));
                String guidanceKey = StudioAiConnectionStatus.nextStep(
                        running, issue, transport, nativeRoute, promptCopied);
                String guidanceText = guidanceKey == null ? "" : StudioBundle.message(guidanceKey);
                if (issue != null) guidanceText += " " + issue;
                if (!next.getText().equals(guidanceText)) next.setText(guidanceText);
                next.setVisible(!guidanceText.isEmpty());
            };
            tabs.addChangeListener(event -> refreshStatus.run());
            refreshStatus.run();
            Timer timer = new Timer(1000, event -> refreshStatus.run());
            timer.start();
            Disposer.register(getDisposable(), timer::stop);
            footer.add(guidance, BorderLayout.CENTER);
            panel.add(footer, BorderLayout.SOUTH);
            return panel;
        }
        private static JBTextArea text(String value) {
            JBTextArea text = new StudioAiConnectionText(value);
            text.setEditable(false); text.setColumns(65); text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
            text.setOpaque(false);
            text.setFont(UIManager.getFont("Label.font"));
            text.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.ConnectionTitle"));
            return text;
        }
        @Override protected Action @NotNull [] createActions() {
            return new Action[]{new AbstractAction(StudioBundle.message("ai.CopyPrompt")) {
                @Override public void actionPerformed(ActionEvent event) {
                    CopyPasteManager.getInstance().setContents(new StringSelection(StudioBundle.message("ai.TestPrompt")));
                    promptCopied = true;
                    refreshStatus.run();
                }
            }, new AbstractAction(StudioBundle.message("ai.Stop")) {
                { putValue(Action.SHORT_DESCRIPTION, StudioBundle.message("ai.StopTooltip")); }
                @Override public void actionPerformed(ActionEvent event) { PropertiesComponent.getInstance(project).setValue(StudioAiStartupActivity.RECONNECT, "false");
                    service.stop(); close(OK_EXIT_CODE); }
            }, getOKAction()};
        }
    }
}
