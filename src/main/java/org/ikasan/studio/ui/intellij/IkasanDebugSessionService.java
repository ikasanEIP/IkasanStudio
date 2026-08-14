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

/** Tracks live Studio module launches for this project, including the debug subset. */
@Service(Service.Level.PROJECT)
public final class IkasanDebugSessionService implements Disposable {
    private final Project project;
    private final Set<ProcessHandler> moduleProcesses =
            Collections.newSetFromMap(new IdentityHashMap<>());
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
                if (isStudioModuleExecution(environment)) {
                    updateProcess(handler, true, isStudioDebugExecution(executorId, environment));
                }
            }

            @SuppressWarnings("NullableProblems")
            @Override
            public void processTerminated(String executorId, ExecutionEnvironment environment,
                                          ProcessHandler handler, int exitCode) {
                updateProcess(handler, false, false);
            }
        });
        registerAlreadyRunningDebugProcesses();
    }

    public synchronized boolean isDebugModuleRunning() {
        return !debugProcesses.isEmpty();
    }

    public synchronized boolean isModuleRunning() {
        return !moduleProcesses.isEmpty();
    }

    public synchronized boolean canStopModule() {
        return moduleProcesses.stream()
                .anyMatch(handler -> !handler.isProcessTerminated() && !handler.isProcessTerminating());
    }

    /** Requests termination of the Studio module processes owned by this project. */
    public void stopModule() {
        Set<ProcessHandler> processes;
        synchronized (this) {
            processes = Collections.newSetFromMap(new IdentityHashMap<>());
            processes.addAll(moduleProcesses);
        }
        for (ProcessHandler handler : processes) {
            if (!handler.isProcessTerminated() && !handler.isProcessTerminating()) {
                handler.destroyProcess();
            }
        }
        repaintCanvas();
    }

    static boolean isStudioModuleExecution(ExecutionEnvironment environment) {
        if (environment == null) {
            return false;
        }
        RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
        return settings != null && isStudioModuleConfiguration(settings.getConfiguration());
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
        if (!isStudioModuleConfiguration(runProfile)) {
            return false;
        }
        ApplicationConfiguration configuration = (ApplicationConfiguration) runProfile;
        return IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS
                .equals(configuration.getProgramParameters());
    }

    private static boolean isStudioModuleConfiguration(RunProfile runProfile) {
        if (!(runProfile instanceof ApplicationConfiguration configuration)) {
            return false;
        }
        return IkasanRunConfigurationService.MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && configuration.getConfigurationModule().getModule() != null;
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
                    moduleProcesses.add(handler);
                    debugProcesses.add(handler);
                }
            }
        }
    }

    private void updateProcess(ProcessHandler handler, boolean running, boolean debug) {
        boolean visibilityChanged;
        synchronized (this) {
            boolean wasModuleRunning = !moduleProcesses.isEmpty();
            boolean wasDebugRunning = !debugProcesses.isEmpty();
            if (running) {
                moduleProcesses.add(handler);
                if (debug) {
                    debugProcesses.add(handler);
                }
            } else {
                moduleProcesses.remove(handler);
                debugProcesses.remove(handler);
            }
            visibilityChanged = wasModuleRunning != !moduleProcesses.isEmpty()
                    || wasDebugRunning != !debugProcesses.isEmpty();
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
        moduleProcesses.clear();
        debugProcesses.clear();
    }
}
