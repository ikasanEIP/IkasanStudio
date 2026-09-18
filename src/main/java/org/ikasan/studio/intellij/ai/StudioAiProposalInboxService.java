package org.ikasan.studio.intellij.ai;

import com.intellij.notification.*;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.util.messages.Topic;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.ui.StudioBundle;
import java.nio.file.*;
import java.util.concurrent.*;

/** Project-local polling also detects files written outside IntelliJ's VFS. All I/O is off the EDT. */
@Service(Service.Level.PROJECT)
public final class StudioAiProposalInboxService implements Disposable {
    public static final Topic<Runnable> CHANGED = Topic.create("Studio AI proposal inbox changed", Runnable.class);
    static final String PENDING = "ikasan.studio.ai.pendingProposal";
    static final String REVISION = "ikasan.studio.ai.pendingProposalRevision";
    private final Project project;
    private final StudioAiProposalInbox inbox = new StudioAiProposalInbox();
    private ScheduledFuture<?> polling;
    private volatile boolean disposed;
    private Notification notification; // EDT only

    public StudioAiProposalInboxService(Project project) { this.project = project; }

    synchronized void start() {
        if (polling == null && !disposed && project.getBasePath() != null) {
            polling = AppExecutorUtil.getAppScheduledExecutorService()
                    .scheduleWithFixedDelay(this::poll, 1, 3, TimeUnit.SECONDS);
        }
    }

    private void poll() {
        if (disposed || project.isDisposed()) return;
        try {
            Path root = Path.of(project.getBasePath());
            if (!Files.isRegularFile(root.resolve("generated/src/main/model/model.json"))) return;
            var files = StudioAiProposalInbox.scan(root.resolve("ai-proposals"));
            inbox.observe(files).ifPresent(path -> {
                var stamp = files.get(path);
                if (!tryAutoApply(path)) {
                    ApplicationManager.getApplication().invokeLater(() -> announce(path, stamp.toString()));
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!disposed && !project.isDisposed() && path.equals(getPendingProposal())) dismissPending();
                    });
                }
            });
        } catch (Exception failure) {
            Logger.getInstance(StudioAiProposalInboxService.class).debug("Could not scan Studio AI proposal inbox", failure);
        }
    }

    private boolean tryAutoApply(Path path) {
        if (org.ikasan.studio.intellij.settings.IkasanStudioSettings.isAlwaysAskAiApproval()) return false;
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(1_048_577);
            if (bytes.length > 1_048_576 || disposed || project.isDisposed()) return false;
            return project.getService(StudioAiService.class)
                    .tryAutoImport(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception failure) {
            // Retain the review banner for stale, incomplete, or temporarily blocked proposals.
            Logger.getInstance(StudioAiProposalInboxService.class).debug("AI proposal needs manual review", failure);
            return false;
        }
    }

    /** EDT-only state access. Only the filename is persisted, scoped to this project. */
    public Path getPendingProposal() {
        String name = PropertiesComponent.getInstance(project).getValue(PENDING);
        if (name == null || project.getBasePath() == null) return null;
        try {
            Path relative = Path.of(name);
            if (relative.isAbsolute() || relative.getNameCount() != 1 || !name.endsWith(".studio-proposal.json")) return null;
            return Path.of(project.getBasePath(), "ai-proposals").resolve(relative);
        } catch (InvalidPathException ignored) { return null; }
    }

    String pendingRevision() { return PropertiesComponent.getInstance(project).getValue(REVISION); }

    public void dismiss(Path expected, String revision) {
        if (disposed || project.isDisposed() || !java.util.Objects.equals(expected, getPendingProposal())
                || !java.util.Objects.equals(revision, pendingRevision())) return;
        PropertiesComponent.getInstance(project).unsetValue(PENDING);
        PropertiesComponent.getInstance(project).unsetValue(REVISION);
        if (notification != null) notification.expire();
        project.getMessageBus().syncPublisher(CHANGED).run();
    }

    public void dismissPending() { dismiss(getPendingProposal(), pendingRevision()); }

    public void reviewPending() {
        Path path = getPendingProposal();
        if (path != null) StudioAiImportProposalAction.openFile(project, path);
    }

    void announce(Path path, String revision) {
        if (disposed || project.isDisposed()) return;
        PropertiesComponent.getInstance(project).setValue(PENDING, path.getFileName().toString());
        PropertiesComponent.getInstance(project).setValue(REVISION, revision);
        project.getMessageBus().syncPublisher(CHANGED).run();
        if (notification != null) notification.expire();
        notification = NotificationGroupManager.getInstance().getNotificationGroup("Ikasan Studio")
                .createNotification(StudioBundle.message("ai.InboxReady"), NotificationType.INFORMATION);
        notification.addAction(NotificationAction.createSimpleExpiring(StudioBundle.message("ai.InboxReview"),
                () -> StudioAiImportProposalAction.openFile(project, path)));
        notification.notify(project);
    }

    @Override public synchronized void dispose() {
        disposed = true;
        if (polling != null) polling.cancel(false);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (notification != null) notification.expire();
        });
    }
}
