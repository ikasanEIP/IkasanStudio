package org.ikasan.studio.ui.intellij;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Tracks live Studio debug launches for this project. */
@Service(Service.Level.PROJECT)
public final class IkasanDebugSessionService implements Disposable {
    private final Project project;
    private final Set<ProcessHandler> debugProcesses =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public IkasanDebugSessionService(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void processStarted(String executorId, ExecutionEnvironment environment,
                                       ProcessHandler handler) {
                if (isStudioDebugExecution(executorId, environment)) {
                    updateProcess(handler, true);
                }
            }

            @SuppressWarnings("NullableProblems")
            @Override
            public void processTerminated(String executorId, ExecutionEnvironment environment,
                                          ProcessHandler handler, int exitCode) {
                updateProcess(handler, false);
            }
        });
        registerAlreadyRunningDebugProcesses();
    }

    public synchronized boolean isDebugModuleRunning() {
        return !debugProcesses.isEmpty();
    }

    static boolean isStudioDebugExecution(String executorId, ExecutionEnvironment environment) {
        if (!DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) || environment == null) {
            return false;
        }
        RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
        return isStudioDebugConfiguration(settings);
    }

    private static boolean isStudioDebugConfiguration(RunnerAndConfigurationSettings settings) {
        return settings != null && isStudioDebugConfiguration(settings.getConfiguration());
    }

    private static boolean isStudioDebugConfiguration(RunProfile runProfile) {
        if (!(runProfile instanceof ApplicationConfiguration configuration)) {
            return false;
        }
        return IkasanRunConfigurationService.MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS
                .equals(configuration.getProgramParameters());
    }

    /**
     * Find debug sessions already running when this service is created (e.g. the IDE was restarted, or the
     * plugin was reloaded, while a "Debug module" launch was still active). Uses XDebuggerManager rather than
     * ExecutionManager.getRunningDescriptors/getExecutors - those are marked @ApiStatus.Internal, whereas
     * XDebuggerManager's session list is public API and already scoped to sessions using the Debug executor.
     */
    private void registerAlreadyRunningDebugProcesses() {
        for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
            if (isStudioDebugConfiguration(session.getRunProfile())) {
                ProcessHandler handler = session.getDebugProcess().getProcessHandler();
                if (!handler.isProcessTerminated()) {
                    debugProcesses.add(handler);
                }
            }
        }
    }

    private void updateProcess(ProcessHandler handler, boolean running) {
        boolean visibilityChanged;
        synchronized (this) {
            boolean wasRunning = !debugProcesses.isEmpty();
            if (running) {
                debugProcesses.add(handler);
            } else {
                debugProcesses.remove(handler);
            }
            boolean isRunningNow = !debugProcesses.isEmpty();
            visibilityChanged = wasRunning != isRunningNow;
        }
        if (visibilityChanged) {
            repaintCanvas();
        }
    }

    private void repaintCanvas() {
        Runnable repaint = () -> {
            if (!project.isDisposed()) {
                DesignerCanvas canvas = project.getService(UiContext.class).getDesignerCanvas();
                if (canvas != null) {
                    canvas.repaint();
                }
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            repaint.run();
        } else {
            ApplicationManager.getApplication().invokeLater(repaint);
        }
    }

    @Override
    public synchronized void dispose() {
        debugProcesses.clear();
    }
}
