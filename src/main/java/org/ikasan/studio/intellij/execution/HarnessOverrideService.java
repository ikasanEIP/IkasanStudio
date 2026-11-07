package org.ikasan.studio.intellij.execution;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.execution.ParametersListUtil;
import org.ikasan.studio.core.model.analysis.HarnessOverrides;
import org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.runtime.TestSftpServerService;
import org.ikasan.studio.ui.UiContext;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/** Selection belongs to each standard Application configuration; live processes retain launch snapshots. */
@Service(Service.Level.PROJECT)
public final class HarnessOverrideService implements Disposable {
    public static final Key<Set<String>> LAUNCH_SELECTION = Key.create("studio.harness.launchSelection");
    public static final Key<Map<String, String>> LAUNCH_LABELS = Key.create("studio.harness.launchLabels");
    private final Project project;
    private final Map<ProcessHandler, Map<String, String>> running = new IdentityHashMap<>();
    public HarnessOverrideService(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment, @NotNull ProcessHandler handler) {
                if (environment.getRunProfile() instanceof ApplicationConfiguration app && isModule(app)) {
                    var snapshot = app.getUserData(LAUNCH_SELECTION);
                    var names = app.getUserData(LAUNCH_LABELS);
                    Map<String, String> captured = new LinkedHashMap<>();
                    if (snapshot != null) for (String id : snapshot) {
                        String label = names == null ? null : names.get(id);
                        if (label == null) label = targets().stream().filter(t -> t.id().equals(id))
                                .map(HarnessOverrides.Target::label).findFirst().orElse(id);
                        captured.put(id, label);
                    }
                    synchronized (running) { running.put(handler, Map.copyOf(captured)); }
                    repaint();
                }
            }
            @Override public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment environment, @NotNull ProcessHandler handler, int exitCode) {
                synchronized (running) { running.remove(handler); }
                repaint();
            }
        });
    }
    public static boolean isModule(ApplicationConfiguration app) {
        return "org.ikasan.studio.boot.Application".equals(app.getMainClassName());
    }
    public ApplicationConfiguration selectedConfiguration() {
        var selected = RunManager.getInstance(project).getSelectedConfiguration();
        return selected != null && selected.getConfiguration() instanceof ApplicationConfiguration app && isModule(app) ? app : null;
    }
    public List<HarnessOverrides.Target> targets() {
        return HarnessOverrides.targets(project.getService(UiContext.class).getIkasanModule());
    }
    public static Set<String> explicitSelection(String vmOptions) {
        for (String option : ParametersListUtil.parse(vmOptions == null ? "" : vmOptions)) {
            if (option.startsWith("-D" + HarnessOverrides.SELECTION + "=")) {
                String value = option.substring(("-D" + HarnessOverrides.SELECTION + "=").length());
                return value.isEmpty() ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(value.split(","))));
            }
        }
        return null;
    }
    public Set<String> selection(ApplicationConfiguration configuration) {
        Set<String> explicit = explicitSelection(configuration == null ? null : configuration.getVMParameters());
        if (explicit != null) return explicit;
        var result = new TreeSet<String>();
        for (var target : targets()) {
            if ("SFTP".equals(target.kind()) && project.getService(TestSftpServerService.class).isLocal()) result.add(target.id());
            // Preserve existing JMS-reader diversion until the developer explicitly manages the selection.
            if ("JMS".equals(target.kind()) && TestJmsHarnessLinks.destinationOverride(
                    project.getService(UiContext.class).getIkasanModule(), target.element()) != null) result.add(target.id());
        }
        return Collections.unmodifiableSet(result);
    }
    public Set<String> nextSelection() { return selection(selectedConfiguration()); }
    public void saveSelection(ApplicationConfiguration configuration, Set<String> selected) {
        var valid = targets().stream().map(HarnessOverrides.Target::id).collect(java.util.stream.Collectors.toSet());
        if (!valid.containsAll(selected)) throw new IllegalArgumentException("Harness selection contains unavailable components");
        configuration.setVMParameters(withSelection(configuration.getVMParameters(), selected));
        repaint();
    }
    public static String withSelection(String vmOptions, Set<String> selected) {
        List<String> options = new ArrayList<>(ParametersListUtil.parse(vmOptions == null ? "" : vmOptions));
        options.removeIf(s -> s.startsWith("-D" + HarnessOverrides.SELECTION + "="));
        options.add("-D" + HarnessOverrides.SELECTION + "=" + String.join(",", new TreeSet<>(selected)));
        return ParametersListUtil.join(options);
    }
    public boolean isEnabled(FlowElement element) { return nextSelection().contains(HarnessOverrides.id(element)); }
    public boolean isActive(FlowElement element) {
        synchronized (running) { return running.values().stream().anyMatch(ids -> ids.containsKey(HarnessOverrides.id(element))); }
    }
    public boolean hasRunningLaunches() { synchronized (running) { return !running.isEmpty(); } }
    public Set<String> activeSelection() {
        synchronized (running) {
            Set<String> all = new TreeSet<>();
            running.values().forEach(labels -> all.addAll(labels.keySet()));
            return Set.copyOf(all);
        }
    }
    public String describe(Set<String> ids) {
        Map<String, String> names = new LinkedHashMap<>();
        targets().forEach(target -> names.put(target.id(), target.label()));
        synchronized (running) { running.values().forEach(labels -> labels.forEach(names::putIfAbsent)); }
        return ids.stream().sorted().map(id -> names.getOrDefault(id, id))
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public void repaint() {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            var canvas = project.getService(UiContext.class).getDesignerCanvas();
            if (canvas != null && !canvas.isDisposed()) { canvas.setInitialiseAllDimensions(true); canvas.repaint(); }
        });
    }
    @Override public void dispose() { synchronized (running) { running.clear(); } }
}
