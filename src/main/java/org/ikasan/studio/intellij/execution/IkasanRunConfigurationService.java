package org.ikasan.studio.intellij.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.intellij.project.StudioProjectFiles;

import java.util.Objects;
import java.util.List;
import com.intellij.util.execution.ParametersListUtil;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import java.util.function.Consumer;

/**
 * Creates, selects, and runs the standard IntelliJ Java Application configuration
 * for the generated Ikasan module entry point.
 */
@Service(Service.Level.PROJECT)
public final class IkasanRunConfigurationService implements Disposable {
    static final String MAIN_CLASS_NAME = "org.ikasan.studio.boot.Application";
    static final String STUDIO_DEBUG_PROGRAM_PARAMETERS = "--spring.profiles.active=studio-debug";
    private static final Logger LOG = Logger.getInstance(IkasanRunConfigurationService.class);

    private final Project project;

    public IkasanRunConfigurationService(Project project) {
        this.project = project;
    }

    /** Outcome of {@link #selectAndRun(String, boolean, Consumer)}, distinguishing why a launch didn't happen. */
    public enum LaunchOutcome {
        LAUNCHED, APPLICATION_FILE_NOT_FOUND, COULD_NOT_CREATE_RUN_CONFIGURATION
    }

    /**
     * Resolves the generated application file and picks the Run/Debug executor internally, so the caller never
     * needs to depend on {@code VirtualFile}/{@code Executor} itself (see
     * ArchUnitBoundaryTest#platformHeavyApisRemainBehindKnownAdapters).
     * @param applicationRelativePath of the generated Application.java, relative to the project root
     * @param debug true to launch via the Debug executor (activating the studio-debug Spring profile), false for a plain Run
     * @param completion told which of {@link LaunchOutcome} resulted
     */
    public void selectAndRun(String applicationRelativePath, boolean debug, Consumer<LaunchOutcome> completion) {
        if (project.isDisposed()) return;
        VirtualFile applicationFile = StudioProjectFiles.getVirtualFile(project, applicationRelativePath);
        if (applicationFile == null) {
            LOG.warn("STUDIO: Could not find " + applicationRelativePath + " in " + project);
            completion.accept(LaunchOutcome.APPLICATION_FILE_NOT_FOUND);
            return;
        }
        Executor executor = debug
                ? DefaultDebugExecutor.getDebugExecutorInstance()
                : DefaultRunExecutor.getRunExecutorInstance();
        selectAndRun(applicationFile, executor, launched ->
                completion.accept(launched ? LaunchOutcome.LAUNCHED : LaunchOutcome.COULD_NOT_CREATE_RUN_CONFIGURATION));
    }

    /**
     * Resolves the owning IntelliJ module away from the EDT, then creates and launches the
     * configuration on the EDT as required by the execution APIs.
     */
    public void selectAndRun(VirtualFile applicationFile, Executor executor, Consumer<Boolean> completion) {
        if (project.isDisposed()) return;
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForFile(applicationFile, project))
                .inSmartMode(project)
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), module -> {
                    if (project.isDisposed()) return;
                    if (module == null) {
                        LOG.warn("STUDIO: No IntelliJ module contains " + applicationFile.getPath());
                        completion.accept(false);
                        return;
                    }
                    completion.accept(selectAndRunOnEdt(module, executor));
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private boolean selectAndRunOnEdt(Module module, Executor executor) {
        boolean debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId());
        var selected = RunManager.getInstance(project).getSelectedConfiguration();
        String environment = selected != null && selected.getConfiguration() instanceof ApplicationConfiguration configuration
                && MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && Objects.equals(configuration.getConfigurationModule().getModule(), module)
                ? environmentOf(configuration.getProgramParameters()) : null;
        java.util.Set<String> harnessSelection = selected != null
                && selected.getConfiguration() instanceof ApplicationConfiguration previous
                && MAIN_CLASS_NAME.equals(previous.getMainClassName())
                && Objects.equals(previous.getConfigurationModule().getModule(), module)
                ? project.getService(HarnessOverrideService.class).selection(previous) : java.util.Set.of();
        RunnerAndConfigurationSettings settings = environment == null
                ? findOrCreateConfiguration(module, debug, harnessSelection)
                : findOrCreateEnvironmentConfiguration(module, debug, environment, harnessSelection);
        RunManager.getInstance(project).setSelectedConfiguration(settings);
        try {
            ExecutionEnvironmentBuilder.create(executor, settings).buildAndExecute();
            return true;
        } catch (ExecutionException e) {
            LOG.warn("STUDIO: Could not execute run configuration " + settings.getName(), e);
            return false;
        }
    }

    /**
     * Run and Debug are kept as separate, independently-cached configurations (rather than one shared
     * configuration launched with different executors) because Debug needs the "studio-debug" Spring profile
     * active - see {@link #STUDIO_DEBUG_PROGRAM_PARAMETERS} - so the generated app's /rest/studio/inject endpoint
     * is only reachable when launched via the Debug button, never via a plain Run or a real deployment.
     */
    RunnerAndConfigurationSettings findOrCreateConfiguration(Module module, boolean debug, java.util.Set<String> harnessSelection) {
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings existing = runManager.getAllSettings().stream()
                .filter(settings -> settings.getConfiguration() instanceof ApplicationConfiguration)
                .filter(settings -> matches(
                        (ApplicationConfiguration) settings.getConfiguration(), module, debug))
                .filter(settings -> Objects.equals(harnessSelection, HarnessOverrideService.explicitSelection(
                        ((ApplicationConfiguration) settings.getConfiguration()).getVMParameters())))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            runManager.setSelectedConfiguration(existing);
            return existing;
        }

        ApplicationConfigurationType type = ApplicationConfigurationType.getInstance();
        ConfigurationFactory factory = type.getConfigurationFactories()[0];
        String configurationName = runManager.suggestUniqueName(
                "Ikasan: " + project.getName() + (debug ? " (Debug)" : ""), type);
        RunnerAndConfigurationSettings settings =
                runManager.createConfiguration(configurationName, factory);
        ApplicationConfiguration configuration =
                (ApplicationConfiguration) settings.getConfiguration();
        configuration.setMainClassName(MAIN_CLASS_NAME);
        configuration.getConfigurationModule().setModule(module);
        if (project.getBasePath() != null) {
            configuration.setWorkingDirectory(project.getBasePath());
        }
        if (debug) {
            configuration.setProgramParameters(STUDIO_DEBUG_PROGRAM_PARAMETERS);
        }

        if (harnessSelection != null) configuration.setVMParameters(
                HarnessOverrideService.withSelection(configuration.getVMParameters(), harnessSelection));
        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        return settings;
    }

    static boolean matches(ApplicationConfiguration configuration, Module module, boolean debug) {
        boolean hasDebugProfile = STUDIO_DEBUG_PROGRAM_PARAMETERS.equals(configuration.getProgramParameters());
        return MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && Objects.equals(configuration.getConfigurationModule().getModule(), module)
                && environmentOf(configuration.getProgramParameters()) == null
                && hasDebugProfile == debug;
    }

    /** Creates local workspace Run/Debug configurations without starting the application. */
    public void prepareEnvironments(List<String> environments, Consumer<Boolean> completion) {
        prepareConfigurations(environments, true, completion);
    }

    /** Standard classpath-based launches for developer-owned imported resources. */
    public void prepareLocalConfiguration(Consumer<Boolean> completion) {
        prepareConfigurations(List.of(""), false, completion);
    }

    private void prepareConfigurations(List<String> environments, boolean externalLocations, Consumer<Boolean> completion) {
        if (project.isDisposed()) return;
        if (environments.isEmpty()) { completion.accept(false); return; }
        var overrides = project.getService(HarnessOverrideService.class);
        var selected = overrides.selectedConfiguration();
        var selection = selected == null ? java.util.Set.<String>of() : overrides.selection(selected);
        var file = StudioProjectFiles.getVirtualFile(project,
                "generated/src/main/java/org/ikasan/studio/boot/Application.java");
        if (file == null) { completion.accept(false); return; }
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForFile(file, project))
                .inSmartMode(project).expireWith(this)
                .finishOnUiThread(ModalityState.nonModal(), module -> {
                    if (project.isDisposed()) return;
                    if (module == null) { completion.accept(false); return; }
                    for (String environment : environments) {
                        if (externalLocations) {
                            findOrCreateEnvironmentConfiguration(module, false, environment, selection);
                            findOrCreateEnvironmentConfiguration(module, true, environment, selection);
                        } else {
                            findOrCreateConfiguration(module, false, selection);
                            findOrCreateConfiguration(module, true, selection);
                        }
                    }
                    RunManager.getInstance(project).setSelectedConfiguration(
                            externalLocations ? findOrCreateEnvironmentConfiguration(module, false, environments.get(0), selection)
                                    : findOrCreateConfiguration(module, false, selection));
                    completion.accept(true);
                }).submit(AppExecutorUtil.getAppExecutorService());
    }

    RunnerAndConfigurationSettings findOrCreateEnvironmentConfiguration(Module module, boolean debug, String environment) {
        return findOrCreateEnvironmentConfiguration(module, debug, environment, java.util.Set.of());
    }

    RunnerAndConfigurationSettings findOrCreateEnvironmentConfiguration(Module module, boolean debug, String environment,
                                                                       java.util.Set<String> harnessSelection) {
        String parameters = environmentParameters(Objects.requireNonNull(project.getBasePath(), "Environment configurations require a project directory"), environment, debug);
        var manager = RunManager.getInstance(project);
        var existing = manager.getAllSettings().stream()
                .filter(s -> s.getConfiguration() instanceof ApplicationConfiguration c
                        && MAIN_CLASS_NAME.equals(c.getMainClassName())
                        && Objects.equals(c.getConfigurationModule().getModule(), module)
                        && parameters.equals(c.getProgramParameters())
                        && Objects.equals(harnessSelection, HarnessOverrideService.explicitSelection(c.getVMParameters())))
                .findFirst();
        if (existing.isPresent()) return existing.get();
        var type = ApplicationConfigurationType.getInstance();
        var settings = manager.createConfiguration(manager.suggestUniqueName("Ikasan: " + project.getName()
                + " — " + (environment.isEmpty() ? "Local" : environment) + (debug ? " (Debug)" : ""), type),
                type.getConfigurationFactories()[0]);
        var configuration = (ApplicationConfiguration) settings.getConfiguration();
        configuration.setMainClassName(MAIN_CLASS_NAME);
        configuration.getConfigurationModule().setModule(module);
        configuration.setWorkingDirectory(project.getBasePath());
        configuration.setProgramParameters(parameters);
        if (harnessSelection != null) configuration.setVMParameters(
                HarnessOverrideService.withSelection(configuration.getVMParameters(), harnessSelection));
        settings.storeInLocalWorkspace();
        manager.addConfiguration(settings);
        return settings;
    }

    static String environmentParameters(String basePath, String environment, boolean debug) {
        // Validate even when called independently of the import UI.
        environment = new EnvironmentProperties.Selection("", environment, false).environment();
        String base = basePath.replace('\\', '/');
        List<String> arguments = new java.util.ArrayList<>();
        arguments.add("--spring.config.additional-location=optional:file:" + base + "/config/,optional:file:" + base + "/config/local/");
        String profiles = environment + (debug ? (environment.isEmpty() ? "" : ",") + "studio-debug" : "");
        if (!profiles.isEmpty()) arguments.add("--spring.profiles.active=" + profiles);
        return ParametersListUtil.join(arguments);
    }

    /** Recognizes only configurations carrying our explicit external configuration argument. */
    static String environmentOf(String parameters) {
        if (parameters == null) return null;
        var arguments = ParametersListUtil.parse(parameters);
        if (arguments.stream().noneMatch(p -> p.startsWith("--spring.config.additional-location=optional:file:")
                && p.contains("/config/,optional:file:") && p.endsWith("/config/local/"))) return null;
        String profiles = arguments.stream().filter(p -> p.startsWith("--spring.profiles.active="))
                .map(p -> p.substring("--spring.profiles.active=".length())).findFirst().orElse("");
        return java.util.Arrays.stream(profiles.split(",")).filter(p -> !p.equals("studio-debug"))
                .findFirst().orElse("");
    }

    @Override
    public void dispose() {
        // Disposed automatically by the platform when the project closes; nothing to release here
        // beyond expiring any in-flight read action via expireWith(this).
    }
}
