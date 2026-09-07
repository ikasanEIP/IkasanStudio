package org.ikasan.studio.intellij.diagnostics;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.core.diagnostics.StudioDiagnosticBundle;
import org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import java.nio.file.Path;
import java.util.Map;

/** Explicit, local-only export available during indexing and without an open designer. */
public final class CollectStudioDiagnosticsAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(CollectStudioDiagnosticsAction.class);
    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed());
    }
    @Override public void actionPerformed(AnActionEvent event) {
        var project = event.getProject();
        if (project == null || project.isDisposed()) return;
        var descriptor = new FileSaverDescriptor(StudioBundle.message("action.IkasanStudio.CollectDiagnostics.text"),
                StudioBundle.message("diagnostics.SaveDescription"), "zip");
        var chosen = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
                .save((VirtualFile) null, "ikasan-studio-diagnostics.zip");
        if (chosen == null) return;
        Path target = chosen.getFile().toPath();
        var plugin = PluginManagerCore.getPlugin(PluginId.getId("com.github.ikasaneip.ikasanstudio"));
        var ide = ApplicationInfo.getInstance();
        var module = project.getService(UiContext.class).getIkasanModule();
        Map<String, String> metadata = Map.of("pluginVersion", plugin == null ? "unavailable" : plugin.getVersion(),
                "ideVersion", ide.getFullVersion(), "ideBuild", ide.getBuild().asString(),
                "metaPack", module == null || module.getMetaVersion() == null ? "unavailable" : module.getMetaVersion());
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                StudioBundle.message("action.IkasanStudio.CollectDiagnostics.text"), false) {
            @Override public void run(ProgressIndicator indicator) {
                try {
                    StudioDiagnosticBundle.write(target, Path.of(PathManager.getLogPath(), "idea.log"), metadata);
                } catch (Exception failure) {
                    if (failure instanceof ProcessCanceledException cancelled) throw cancelled;
                    LOG.warn(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.DIAGNOSTICS_FAILED, failure, null, null, null));
                    throw new DiagnosticsFailure(failure);
                }
            }
            @Override public void onSuccess() {
                LOG.info(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.DIAGNOSTICS_COLLECTED, null, null, null, null));
                if (!project.isDisposed()) StudioUIUtils.displayIdeaInfoMessage(project,
                        StudioBundle.message("diagnostics.Saved", target.toString()));
            }
            @Override public void onThrowable(Throwable failure) {
                // Expected filesystem/configuration failures must not enter IntelliJ's fatal-error reporter.
                if (!project.isDisposed()) StudioUIUtils.displayIdeaWarnMessage(project,
                        StudioBundle.message("diagnostics.Failed"));
            }
        });
    }
    private static final class DiagnosticsFailure extends RuntimeException {
        DiagnosticsFailure(Throwable cause) { super("Could not write local diagnostics archive", cause); }
    }
}
