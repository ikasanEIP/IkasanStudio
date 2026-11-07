package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

/** Starts the project-owned server for explicitly selected SFTP component overrides. */
public final class SftpHarnessAction {
    private SftpHarnessAction() { }
    public static void choose(Project project) {
        org.ikasan.studio.intellij.configuration.HarnessOverridesDialog.show(project);
    }
    public static void start(Project project) {
        var service = project.getService(TestSftpServerService.class);
        var overrides = project.getService(org.ikasan.studio.intellij.execution.HarnessOverrideService.class);
        boolean selected = overrides.targets().stream().anyMatch(t -> "SFTP".equals(t.kind())
                && overrides.nextSelection().contains(t.id()));
        if (!selected) { choose(project); return; }
        service.setLocal(true);
        var module = project.getService(org.ikasan.studio.ui.UiContext.class).getIkasanModule();
        java.util.List<String> directories = new java.util.ArrayList<>();
        if (module != null) for (var flow : module.getFlows()) for (var element : flow.getFlowElementsNoExternalEndPoints()) {
            if ("SFTP Endpoint".equals(element.getComponentMeta().getEndpointKey()))
                directories.add("component-" + org.ikasan.studio.core.model.analysis.HarnessOverrides.id(element));
        }
        background(project, () -> {
            // The project service owns this server and closes it on stop/disposal.
            @SuppressWarnings("resource") var server = service.start();
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
            // The project service owns this server and closes it on stop/disposal.
            @SuppressWarnings("resource") var server = project.getService(TestSftpServerService.class).start();
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
