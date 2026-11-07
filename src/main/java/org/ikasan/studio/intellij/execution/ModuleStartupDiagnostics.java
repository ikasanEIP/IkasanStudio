package org.ikasan.studio.intellij.execution;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Alarm;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/** Observes each module launch before its first output, independently of editor ownership and process liveness. */
@Service(Service.Level.PROJECT)
public final class ModuleStartupDiagnostics implements Disposable {
    private final Project project;
    private final Alarm notifications = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    private final Map<ProcessHandler, Entry> active = new IdentityHashMap<>();
    private volatile ModuleStartupOutput.Failure problem;
    private long generation;
    private volatile boolean debug;
    private volatile boolean disposed;
    private volatile com.intellij.notification.Notification lastNotification;

    public ModuleStartupDiagnostics(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment, @NotNull ProcessHandler handler) {
                synchronized (ModuleStartupDiagnostics.this) {
                    var entry = active.get(handler);
                    if (entry != null && entry.generation == generation)
                        debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executorId);
                }
            }
        });
    }

    public synchronized String message() {
        var failure = problem;
        if (failure == null) return null;
        return failure.port() == null ? StudioBundle.message(failure.prematureShutdown() ? "startup.prematureShutdown" : "startup.failure")
                : StudioBundle.message("startup.portConflict", failure.port().toString());
    }

    public String bannerMessage() {
        var failure = problem;
        if (failure == null) return "";
        return failure.port() == null ? StudioBundle.message(failure.prematureShutdown() ? "startup.prematureShutdownBanner" : "startup.failureBanner")
                : StudioBundle.message("startup.portBanner", failure.port().toString());
    }

    void attach(ProcessHandler handler) {
        Entry entry;
        synchronized (this) {
            if (disposed || project.isDisposed() || active.containsKey(handler)) return;
            problem = null;
            if (lastNotification != null) { lastNotification.expire(); lastNotification = null; }
            entry = new Entry(++generation);
            active.put(handler, entry);
        }
        entry.listener = new ProcessAdapter() {
            @Override public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (entry.stopping) return;
                if (!ProcessOutputType.isStdout(outputType) && !ProcessOutputType.isStderr(outputType)) return;
                var failure = entry.output.accept(event.getText());
                if (failure != null) report(entry, failure);
            }
            @Override public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                entry.stopping = willBeDestroyed;
            }
            @Override public void processTerminated(@NotNull ProcessEvent event) {
                synchronized (ModuleStartupDiagnostics.this) { active.remove(handler); }
                if (!entry.stopping && event.getExitCode() != 0 && !entry.output.ready())
                    report(entry, entry.output.failure() == null ? new ModuleStartupOutput.Failure(null) : entry.output.failure());
                handler.removeProcessListener(this);
            }
        };
        handler.addProcessListener(entry.listener);
        synchronized (this) {
            if (disposed) { handler.removeProcessListener(entry.listener); active.remove(handler); }
        }
    }

    private void report(Entry entry, ModuleStartupOutput.Failure failure) {
        final long revision;
        synchronized (this) {
            if (disposed || project.isDisposed() || notifications.isDisposed()
                    || entry.generation != generation || Objects.equals(entry.reported, failure)) return;
            entry.reported = failure;
            problem = failure;
            revision = ++entry.revision;
        }
        // Spring's failure banner and its description can arrive as separate chunks.
        notifications.addRequest(() -> ApplicationManager.getApplication().invokeLater(() -> {
            synchronized (ModuleStartupDiagnostics.this) {
                if (disposed || project.isDisposed() || entry.generation != generation || entry.revision != revision) return;
                if (entry.notification != null) entry.notification.expire();
                entry.notification = NotificationGroupManager.getInstance()
                        .getNotificationGroup(StudioUIUtils.NOTIFICATION_GROUP_ID)
                        .createNotification(StudioBundle.message("startup.title"), failure.port() == null
                                ? StudioBundle.message(failure.prematureShutdown() ? "startup.prematureShutdown" : "startup.failure")
                                : StudioBundle.message("startup.portConflict", failure.port().toString()), NotificationType.ERROR)
                        .addAction(NotificationAction.createSimpleExpiring(StudioBundle.message("startup.output"), this::showOutput));
                lastNotification = entry.notification;
                entry.notification.notify(project);
            }
        }), 250);
    }

    public void showOutput() {
        if (disposed || project.isDisposed()) return;
        var window = ToolWindowManager.getInstance(project).getToolWindow(debug ? "Debug" : "Run");
        if (window != null) window.activate(null);
    }

    @Override public synchronized void dispose() {
        disposed = true;
        notifications.cancelAllRequests();
        active.forEach((handler, entry) -> {
            if (entry.listener != null) handler.removeProcessListener(entry.listener);
            if (entry.notification != null) entry.notification.expire();
        });
        active.clear();
        if (lastNotification != null) { lastNotification.expire(); lastNotification = null; }
        problem = null;
    }

    private static final class Entry {
        private final long generation;
        private final ModuleStartupOutput output = new ModuleStartupOutput();
        private ModuleStartupOutput.Failure reported;
        private long revision;
        private volatile boolean stopping;
        private ProcessAdapter listener;
        private com.intellij.notification.Notification notification;
        Entry(long generation) { this.generation = generation; }
    }
}
