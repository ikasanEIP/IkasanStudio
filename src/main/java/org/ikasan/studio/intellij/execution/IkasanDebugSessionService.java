package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.intellij.runtime.FlowErrorMonitorService;

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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.integration.ikasan.ModuleControlClient;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Tracks live Studio module launches for this project, including the debug subset. */
@Service(Service.Level.PROJECT)
public final class IkasanDebugSessionService implements Disposable {
    private static final Logger LOG = Logger.getInstance(IkasanDebugSessionService.class);
    // How often to re-probe the module's own REST interface while waiting for it to come up - see
    // probeModuleReachability(). A plain TCP/HTTP round-trip is cheap enough that 1s is comfortably responsive
    // without hammering a module that's still deep in Spring context startup (which the existing
    // "ModuleNotYetAcceptingConnections" message already notes can take a while, longer still for JMS-backed
    // consumers) - and this alarm chain unconditionally stops the moment the module either answers once or its
    // process terminates, so it never runs for longer than a single debug session actually needs it to.
    private static final int REACHABILITY_PROBE_INTERVAL_MS = 1000;

    private final Project project;
    private final Set<ProcessHandler> moduleProcesses =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ProcessHandler> debugProcesses =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Alarm reachabilityAlarm;
    private boolean moduleReachable = false;
    private volatile boolean disposed;

    public IkasanDebugSessionService(Project project) {
        this.project = project;
        this.reachabilityAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
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

    /**
     * True once the debug module has actually answered its own REST interface at least once this session - as
     * opposed to {@link #isDebugModuleRunning()}, which only reflects the JVM process being alive and can stay
     * true for a while before the Spring context has finished starting and the app is genuinely accepting
     * connections. Gates the canvas's Send Test Message / Trigger Now badges (see
     * {@code IkasanFlowRouteViewHandler#paintSendTestMessageBadge}) so they don't appear - and immediately fail
     * if clicked - during that startup window.
     */
    public synchronized boolean isModuleReachable() {
        return moduleReachable;
    }

    /** Every call site wants this in the negative sense (a guard/bail condition), hence the positive-sense antonym name rather than isModuleRunning() + "!" at every use. */
    public synchronized boolean isModuleStopped() {
        return moduleProcesses.isEmpty();
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
        boolean debugAlreadyRunning = false;
        for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
            if (isStudioDebugConfiguration(session.getRunProfile())) {
                ProcessHandler handler = session.getDebugProcess().getProcessHandler();
                if (!handler.isProcessTerminated()) {
                    moduleProcesses.add(handler);
                    debugProcesses.add(handler);
                    debugAlreadyRunning = true;
                }
            }
        }
        // The IDE (or just this plugin) may have restarted mid-session while a debug launch was still active -
        // no processStarted event will ever fire for it, so the reachability probe needs starting here too,
        // otherwise the Send Test Message badge would simply never appear for the rest of that debug session.
        if (debugAlreadyRunning) {
            project.getService(FlowErrorMonitorService.class).moduleProcessStarted();
            scheduleReachabilityProbe();
        }
    }

    private void updateProcess(ProcessHandler handler, boolean running, boolean debug) {
        boolean visibilityChanged;
        boolean moduleStopped;
        boolean moduleStarted;
        boolean debugStarted;
        boolean debugStopped;
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
            boolean isDebugRunningNow = !debugProcesses.isEmpty();
            boolean isModuleRunningNow = !moduleProcesses.isEmpty();
            moduleStopped = wasModuleRunning && !isModuleRunningNow;
            moduleStarted = !wasModuleRunning && isModuleRunningNow;
            visibilityChanged = wasModuleRunning != isModuleRunningNow || wasDebugRunning != isDebugRunningNow;
            debugStarted = !wasDebugRunning && isDebugRunningNow;
            debugStopped = wasDebugRunning && !isDebugRunningNow;
            if (debugStopped) {
                moduleReachable = false;
            }
        }
        if (debugStopped) {
            reachabilityAlarm.cancelAllRequests();
        } else if (debugStarted) {
            scheduleReachabilityProbe();
        }
        if (moduleStarted) {
            project.getService(FlowErrorMonitorService.class).moduleProcessStarted();
        } else if (moduleStopped) {
            project.getService(FlowErrorMonitorService.class).moduleProcessStopped();
        }
        if (visibilityChanged) {
            repaintCanvas();
        }
    }

    private void scheduleReachabilityProbe() {
        if (!disposed && !reachabilityAlarm.isDisposed()) {
            reachabilityAlarm.addRequest(this::probeModuleReachability, REACHABILITY_PROBE_INTERVAL_MS);
        }
    }

    /**
     * One reachability check, on the alarm's pooled thread - stops rescheduling itself the moment either the
     * module answers (success - {@link #moduleReachable} flips true, badge appears on the next repaint) or the
     * debug process is no longer running (see {@link #updateProcess}, which cancels this alarm directly on
     * process termination - the isDebugModuleRunning() re-check here is a second guard against a probe that was
     * already in flight when that cancellation happened).
     */
    private void probeModuleReachability() {
        if (disposed || !isDebugModuleRunning() || project.isDisposed()) {
            return;
        }
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || module.getIdentity() == null || module.getPort() == null) {
            scheduleReachabilityProbe();
            return;
        }
        try {
            ModuleControlClient.fetchFlowStates(module);
        } catch (Exception e) {
            // Expected throughout the module's startup window - not worth logging above debug, see CLAUDE.md's
            // "never log above warn" rule and ModuleControlClient's own identical reasoning.
            LOG.debug("STUDIO: module not yet reachable via its REST interface: " + e);
            scheduleReachabilityProbe();
            return;
        }
        synchronized (this) {
            moduleReachable = true;
        }
        repaintCanvas();
    }

    private void repaintCanvas() {
        Runnable repaint = () -> {
            if (!disposed && !project.isDisposed()) {
                DesignerCanvas canvas = project.getService(UiContext.class).getDesignerCanvas();
                if (canvas != null && !canvas.isDisposed()) {
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
        disposed = true;
        reachabilityAlarm.cancelAllRequests();
        moduleProcesses.clear();
        debugProcesses.clear();
    }
}
