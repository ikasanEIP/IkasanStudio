package org.ikasan.studio.ui.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
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

import java.util.Objects;
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

    /**
     * Resolves the owning IntelliJ module away from the EDT, then creates and launches the
     * configuration on the EDT as required by the execution APIs.
     */
    public void selectAndRun(VirtualFile applicationFile, Executor executor, Consumer<Boolean> completion) {
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForFile(applicationFile, project))
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), module -> {
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
        RunnerAndConfigurationSettings settings = findOrCreateConfiguration(module, debug);
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
     * active - see {@link #STUDIO_DEBUG_PROGRAM_PARAMETERS} - so the generated app's /studio/inject endpoint
     * is only reachable when launched via the Debug button, never via a plain Run or a real deployment.
     */
    RunnerAndConfigurationSettings findOrCreateConfiguration(Module module, boolean debug) {
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings existing = runManager.getAllSettings().stream()
                .filter(settings -> settings.getConfiguration() instanceof ApplicationConfiguration)
                .filter(settings -> matches(
                        (ApplicationConfiguration) settings.getConfiguration(), module, debug))
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

        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        return settings;
    }

    static boolean matches(ApplicationConfiguration configuration, Module module, boolean debug) {
        boolean hasDebugProfile = STUDIO_DEBUG_PROGRAM_PARAMETERS.equals(configuration.getProgramParameters());
        return MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && Objects.equals(configuration.getConfigurationModule().getModule(), module)
                && hasDebugProfile == debug;
    }

    @Override
    public void dispose() {
        // Disposed automatically by the platform when the project closes; nothing to release here
        // beyond expiring any in-flight read action via expireWith(this).
    }
}
