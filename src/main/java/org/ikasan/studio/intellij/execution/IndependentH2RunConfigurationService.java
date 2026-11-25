package org.ikasan.studio.intellij.execution;

import com.intellij.execution.*;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.Topic;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ikasan.studio.core.generator.IndependentH2Template;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

/** Owns the independent development server's actual Java processes, separately from editor/menu lifetimes. */
@Service(Service.Level.PROJECT)
public final class IndependentH2RunConfigurationService implements Disposable {
    public static final String MAIN_CLASS = IndependentH2Template.PACKAGE + "." + IndependentH2Template.CLASS_NAME;
    public enum State { STOPPED, STARTING, RUNNING, STOPPING }
    public interface Listener { void changed(); }
    public static final Topic<Listener> TOPIC = Topic.create("Studio independent H2 state", Listener.class);
    private static final Pattern READY = Pattern.compile("(?m)^IKASAN_STUDIO_H2_READY tcp=([0-9]+) web=([0-9]+)\\r?\\n");
    private final Project project;
    private final ConcurrentHashMap<ProcessHandler, OwnedProcess> processes = new ConcurrentHashMap<>();
    private volatile boolean preparing;
    private volatile boolean disposed;

    private static final class OwnedProcess {
        final StringBuilder output = new StringBuilder();
        volatile boolean ready, stopping;
        volatile ScheduledFuture<?> stopTimeout;
    }

    public IndependentH2RunConfigurationService(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment environment) {
                if (isOurs(environment.getRunProfile())) { preparing = true; publish(); }
            }
            @Override public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment) {
                if (isOurs(environment.getRunProfile())) { preparing = false; publish(); }
            }
        });
    }

    static boolean isOurs(Object configuration) {
        return configuration instanceof ApplicationConfiguration app && MAIN_CLASS.equals(app.getMainClassName());
    }

    public State state() {
        if (processes.values().stream().anyMatch(p -> p.ready && !p.stopping)) return State.RUNNING;
        if (processes.values().stream().anyMatch(p -> p.stopping)) return State.STOPPING;
        return preparing || !processes.isEmpty() ? State.STARTING : State.STOPPED;
    }

    public void listen(Listener listener, Disposable parent) {
        project.getMessageBus().connect(parent).subscribe(TOPIC, listener);
    }

    public void toggle() {
        if (disposed || project.isDisposed()) return;
        if (state() == State.RUNNING) requestStop();
        else if (state() == State.STOPPED) start();
    }

    private void start() {
        org.ikasan.studio.core.model.ikasan.instance.Module model = project.getService(UiContext.class).getIkasanModule();
        if (model == null) { error("h2.independent.noModel"); return; }
        String parameters;
        try { parameters = parameters(model.getH2PortNumber(), model.getH2WebPortNumber()); }
        catch (IllegalArgumentException invalid) { error("h2.independent.invalidPorts"); return; }
        VirtualFile file = StudioProjectFiles.getVirtualFile(project, IndependentH2Template.PATH);
        if (file == null) { error("h2.independent.regenerate"); return; }
        preparing = true; publish();
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForFile(file, project))
                .inSmartMode(project).expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), module -> {
                    if (disposed || project.isDisposed()) return;
                    if (module == null) { preparing = false; publish(); error("h2.independent.reloadMaven"); return; }
                    RunnerAndConfigurationSettings settings = findOrCreate(module, parameters);
                    RunManager.getInstance(project).setSelectedConfiguration(settings);
                    try {
                        ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings).buildAndExecute();
                    } catch (ExecutionException failure) {
                        preparing = false; publish(); error("h2.independent.launchFailed");
                    }
                }).submit(AppExecutorUtil.getAppExecutorService());
    }

    static String parameters(String tcp, String web) {
        int first = Integer.parseInt(tcp), second = Integer.parseInt(web);
        if (first < 1 || first > 65535 || second < 1 || second > 65535 || first == second)
            throw new IllegalArgumentException("Invalid H2 ports");
        return first + " " + second;
    }

    RunnerAndConfigurationSettings findOrCreate(Module module, String parameters) {
        RunManager manager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = manager.getAllSettings().stream().filter(s -> isOurs(s.getConfiguration())
                && Objects.equals(((ApplicationConfiguration) s.getConfiguration()).getConfigurationModule().getModule(), module))
                .findFirst().orElseGet(() -> {
                    ApplicationConfigurationType type = ApplicationConfigurationType.getInstance();
                    RunnerAndConfigurationSettings created = manager.createConfiguration(manager.suggestUniqueName(
                            StudioBundle.message("h2.independent.configuration", project.getName()), type), type.getConfigurationFactories()[0]);
                    ApplicationConfiguration app = (ApplicationConfiguration) created.getConfiguration();
                    app.setMainClassName(MAIN_CLASS); app.getConfigurationModule().setModule(module);
                    app.setWorkingDirectory(project.getBasePath());
                    app.setAllowRunningInParallel(false);
                    created.storeInLocalWorkspace(); manager.addConfiguration(created); return created;
                });
        ((ApplicationConfiguration) settings.getConfiguration()).setProgramParameters(parameters);
        return settings;
    }

    /** Called by the Java run extension before process notifications, including launches from the IDE Run menu. */
    void attach(ProcessHandler handler) {
        OwnedProcess entry = new OwnedProcess();
        if (processes.putIfAbsent(handler, entry) != null) return;
        preparing = false;
        handler.addProcessListener(new ProcessListener() {
            @Override public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (!ProcessOutputTypes.STDOUT.equals(outputType) || entry.ready) return;
                synchronized (entry.output) {
                    entry.output.append(event.getText());
                    Matcher ready = READY.matcher(entry.output);
                    if (ready.find()) {
                        entry.ready = true; publish();
                        int port = Integer.parseInt(ready.group(2));
                        if (port > 0 && port <= 65535) ui(() -> {
                            if (!entry.stopping && processes.containsKey(handler)) BrowserUtil.browse("http://127.0.0.1:" + port + "/");
                        });
                    }
                    if (entry.output.length() > 8192) entry.output.delete(0, entry.output.length() - 4096);
                }
            }
            @Override public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                entry.stopping = true; publish();
            }
            @Override public void processTerminated(@NotNull ProcessEvent event) {
                processes.remove(handler);
                if (entry.stopTimeout != null) entry.stopTimeout.cancel(false);
                publish();
                if (!entry.ready && event.getExitCode() != 0) ui(() -> error("h2.independent.startFailed"));
            }
        });
        if (handler.isProcessTerminated()) processes.remove(handler);
        publish();
    }

    public void requestStop() {
        processes.forEach((handler, entry) -> {
            if (handler.isProcessTerminated() || entry.stopping) return;
            entry.stopping = true;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    OutputStream input = handler.getProcessInput();
                    if (input == null) handler.destroyProcess();
                    else { input.write("STOP\n".getBytes(StandardCharsets.UTF_8)); input.flush(); }
                } catch (IOException failure) { handler.destroyProcess(); }
            });
            entry.stopTimeout = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> ui(() -> {
                if (handler.isProcessTerminated()) return;
                if (Messages.showYesNoDialog(project, StudioBundle.message("h2.independent.forceStop"),
                        StudioBundle.message("h2.independent.title"), Messages.getWarningIcon()) == Messages.YES) {
                    if (handler instanceof OSProcessHandler os) os.getProcess().destroyForcibly();
                    else handler.destroyProcess();
                }
            }), 5, TimeUnit.SECONDS);
        });
        publish();
    }

    private void error(String key) { StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message(key)); }
    private void ui(Runnable action) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!disposed && !project.isDisposed()) action.run();
        }, ModalityState.any());
    }
    private void publish() { ui(() -> project.getMessageBus().syncPublisher(TOPIC).changed()); }

    @Override public void dispose() {
        disposed = true; preparing = false;
        processes.forEach((handler, entry) -> {
            if (entry.stopTimeout != null) entry.stopTimeout.cancel(false);
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    OutputStream input = handler.getProcessInput();
                    if (input != null) { input.write("STOP\n".getBytes(StandardCharsets.UTF_8)); input.flush(); }
                } catch (IOException ignored) { }
                if (!handler.isProcessTerminated()) handler.destroyProcess();
            });
        });
    }
}
