package org.ikasan.studio.intellij.testing;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.Alarm;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Editor-owned warning, recomputed on open and saved configuration changes. No IO on the EDT. */
public final class FlowTestPropertiesBanner extends EditorNotificationPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(FlowTestPropertiesBanner.class);
    private final Project project;
    private final Alarm checks = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    private final AtomicLong revision = new AtomicLong();
    private volatile boolean disposed;
    private FlowTestPropertyWarnings.Status status = new FlowTestPropertyWarnings.Status(List.of(), false); // EDT only

    public FlowTestPropertiesBanner(Project project) {
        this.project = project;
        setText(StudioBundle.message("flowTest.renameWarning.banner"));
        icon(AllIcons.General.Warning);
        createActionLabel(StudioBundle.message("flowTest.setupWarning.action"), () -> {
            if (!status.brokenReferences().isEmpty()) GenerateFlowTestAction.openPropertiesRefresh(project);
            else GenerateFlowTestAction.open(project, null);
        });
        createActionLabel(StudioBundle.message("flowTest.renameWarning.details"), () -> {
            String details = status.staleSupport() ? StudioBundle.message("flowTest.setupWarning.supportHelp") : "";
            if (!status.brokenReferences().isEmpty()) details += "\n\n"
                    + StudioBundle.message("flowTest.renameWarning.body") + "\n\nmodule-test.properties\n"
                    + String.join("\n", status.brokenReferences());
            Messages.showInfoMessage(project, details.trim(), StudioBundle.message("flowTest.setupWarning.title"));
        });
        setVisible(false);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
                VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                    @Override public void after(@NotNull List<? extends VFileEvent> events) {
                        String root = project.getBasePath();
                        if (root != null && events.stream().anyMatch(event ->
                                event.getPath().equals(root + "/" + FlowTestScaffold.TEST_PROPERTIES_PATH)
                                || event.getPath().equals(root + "/generated/src/main/resources/application.properties")
                                || event.getPath().equals(root + "/generated/src/main/model/model.json")
                                || event.getPath().equals(root + "/" + FlowTestScaffold.SUPPORT_PATH))) {
                            requestCheck();
                        }
                    }
                });
        requestCheck();
    }

    public void requestCheck() {
        if (disposed || project.isDisposed() || project.getBasePath() == null) return;
        long requested = revision.incrementAndGet();
        Path root = Path.of(project.getBasePath());
        checks.cancelAllRequests();
        checks.addRequest(() -> {
            try {
                var checked = FlowTestPropertyWarnings.inspect(root);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (disposed || project.isDisposed() || revision.get() != requested) return;
                    status = checked;
                    setText(StudioBundle.message(checked.staleSupport()
                            ? (checked.brokenReferences().isEmpty() ? "flowTest.setupWarning.support" : "flowTest.setupWarning.both")
                            : "flowTest.renameWarning.banner"));
                    setVisible(checked.needsRefresh());
                    revalidate();
                    if (getParent() != null) { getParent().revalidate(); getParent().repaint(); }
                });
            } catch (Exception failure) {
                LOG.warn("Could not check flow test properties", failure);
            }
        }, 200);
    }

    @Override public void dispose() { disposed = true; revision.incrementAndGet(); }
}
