package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.integration.ikasan.StudioInjectClient;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Reads the module's live scan configuration without doing network work on the EDT. */
public final class ShowLocalFileScanDirectoryAction implements ActionListener {
    private final Project project;
    private final FlowElement consumer;

    public ShowLocalFileScanDirectoryAction(Project project, FlowElement consumer) {
        this.project = project;
        this.consumer = consumer;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (!project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.LocalFileScanRequiresDebug"));
            return;
        }
        var module = project.getService(UiContext.class).getIkasanModule();
        String flowName = consumer.getContainingFlow().getIdentity();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var response = StudioInjectClient.getScanDirectories(module, flowName);
                if (response.statusCode() != 200) {
                    throw new IllegalStateException(response.statusCode() + ": " + response.body()
                            + (response.statusCode() == 404 ? " (Update Code and restart Debug module to install the scan-directory endpoint.)" : ""));
                }
                var body = new ObjectMapper().readTree(response.body());
                var directories = new java.util.ArrayList<String>();
                body.path("directories").forEach(path -> directories.add(path.asText()));
                String details = StudioBundle.message("message.LocalFileScanDetails", String.join("\n", directories),
                        body.path("patterns").toString(), body.path("directoryDepth").asText());
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) Messages.showInfoMessage(project, details,
                            StudioBundle.message("dialog.LocalFileScanDirectories"));
                });
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                            StudioBundle.message("message.LocalFileScanFailed", e.getMessage()));
                });
            }
        });
    }
}
