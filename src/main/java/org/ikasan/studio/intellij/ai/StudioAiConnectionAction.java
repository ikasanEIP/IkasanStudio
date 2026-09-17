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
    private static final class ConnectionDialog extends DialogWrapper {
        private final StudioAiService service;
        private final String configuration;
        ConnectionDialog(Project project, StudioAiService service, String configuration) {
            super(project, false); this.service = service; this.configuration = configuration;
            setModal(false);
            setTitle(StudioBundle.message("ai.ConnectionTitle"));
            setOKButtonText(StudioBundle.message("button.Close"));
            init();
        }
        @Override protected JComponent createCenterPanel() {
            JBTextArea text = new JBTextArea(StudioBundle.message("ai.ConnectionExplanation") + "\n\n" + configuration);
            text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
            text.getAccessibleContext().setAccessibleName(StudioBundle.message("ai.ConnectionTitle"));
            JBScrollPane pane = new JBScrollPane(text); pane.setPreferredSize(JBUI.size(720, 380)); return pane;
        }
        @Override protected Action @NotNull [] createActions() {
            return new Action[]{new AbstractAction(StudioBundle.message("ai.CopyConfiguration")) {
                @Override public void actionPerformed(ActionEvent event) {
                    CopyPasteManager.getInstance().setContents(new StringSelection(configuration));
                }
            }, new AbstractAction(StudioBundle.message("ai.Stop")) {
                @Override public void actionPerformed(ActionEvent event) { service.stop(); close(OK_EXIT_CODE); }
            }, getOKAction()};
        }
    }
}
