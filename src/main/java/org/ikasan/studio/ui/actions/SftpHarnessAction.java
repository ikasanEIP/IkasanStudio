package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

/** Native chooser for the project-wide local/external SFTP development mode. */
public final class SftpHarnessAction {
    private SftpHarnessAction() { }
    public static void choose(Project project) {
        var running = project.getService(IkasanDebugSessionService.class);
        if (running.isRunModuleRunning() || running.isDebugModuleRunning()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("sftpHarness.stopModule"));
            return;
        }
        var service = project.getService(TestSftpServerService.class);
        int choice = Messages.showDialog(project, StudioBundle.message("sftpHarness.help"),
                StudioBundle.message("sftpHarness.title"),
                new String[]{StudioBundle.message("sftpHarness.local"), StudioBundle.message("sftpHarness.external"), StudioBundle.message("sftpBrowser.close")},
                service.isSelected() && !service.isLocal() ? 1 : 0, Messages.getQuestionIcon());
        if (choice == 0) { service.setLocal(true); start(project); }
        if (choice == 1) { service.setLocal(false); stop(project); }
    }
    public static void start(Project project) {
        var service = project.getService(TestSftpServerService.class);
        if (!service.isSelected()) { choose(project); return; }
        if (!service.isLocal()) return;
        var module = project.getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule();
        java.util.List<String> directories = new java.util.ArrayList<>();
        if (module != null) for (var flow : module.getFlows()) for (var element : flow.getFlowElementsNoExternalEndPoints()) {
            if ("SFTP Endpoint".equals(element.getComponentMeta().getEndpointKey()))
                directories.add(org.ikasan.studio.integration.sftp.LocalSftpHarness.directory(flow.getIdentity(), element.getIdentity()));
        }
        background(project, () -> {
            var server = service.start();
            for (String directory : directories) java.nio.file.Files.createDirectories(server.home().resolve(directory));
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) StudioUIUtils.displayIdeaInfoMessage(project,
                        StudioBundle.message("sftpHarness.started", server.port(), server.home()));
            });
        });
    }
    public static void stop(Project project) {
        background(project, () -> project.getService(TestSftpServerService.class).stop());
    }
    public static void showFiles(Project project) {
        background(project, () -> {
            var server = project.getService(TestSftpServerService.class).start();
            StudioNavigator.selectInProjectView(project, server.home());
        });
    }
    private static void background(Project project, Work work) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try { if (!project.isDisposed()) work.run(); }
            catch (Exception failure) {
                String detail = StopTestMailServerAction.redact(failure.getMessage() == null
                        ? failure.getClass().getSimpleName() : failure.getMessage());
                com.intellij.openapi.diagnostic.Logger.getInstance(SftpHarnessAction.class).warn("SFTP harness: " + detail);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) StudioUIUtils.displayIdeaErrorMessage(project,
                            StudioBundle.message("sftpHarness.failed", detail));
                });
            }
        });
    }
    private interface Work { void run() throws Exception; }
}
