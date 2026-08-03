package org.ikasan.studio.ui.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Creates, selects, and runs the standard IntelliJ Java Application configuration
 * for the generated Ikasan module entry point.
 */
@Service(Service.Level.PROJECT)
public final class IkasanRunConfigurationService implements Disposable {
    static final String MAIN_CLASS_NAME = "org.ikasan.studio.boot.Application";
    private static final Logger LOG = Logger.getInstance(IkasanRunConfigurationService.class);

    private final Project project;

    public IkasanRunConfigurationService(Project project) {
        this.project = project;
    }

    /**
     * Resolves the owning IntelliJ module away from the EDT, then creates and launches the
     * configuration on the EDT as required by the execution APIs.
     */
    public void selectAndRun(VirtualFile applicationFile, Consumer<Boolean> completion) {
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForFile(applicationFile, project))
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), module -> {
                    if (module == null) {
                        LOG.warn("STUDIO: No IntelliJ module contains " + applicationFile.getPath());
                        completion.accept(false);
                        return;
                    }
                    completion.accept(selectAndRunOnEdt(module));
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private boolean selectAndRunOnEdt(Module module) {
        RunnerAndConfigurationSettings settings = findOrCreateConfiguration(module);
        RunManager.getInstance(project).setSelectedConfiguration(settings);
        try {
            ExecutionEnvironmentBuilder.create(
                    DefaultRunExecutor.getRunExecutorInstance(), settings).buildAndExecute();
            return true;
        } catch (ExecutionException e) {
            LOG.warn("STUDIO: Could not execute run configuration " + settings.getName(), e);
            return false;
        }
    }

    RunnerAndConfigurationSettings findOrCreateConfiguration(Module module) {
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings existing = runManager.getAllSettings().stream()
                .filter(settings -> settings.getConfiguration() instanceof ApplicationConfiguration)
                .filter(settings -> matches(
                        (ApplicationConfiguration) settings.getConfiguration(), module))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            runManager.setSelectedConfiguration(existing);
            return existing;
        }

        ApplicationConfigurationType type = ApplicationConfigurationType.getInstance();
        ConfigurationFactory factory = type.getConfigurationFactories()[0];
        String configurationName = runManager.suggestUniqueName(
                "Ikasan: " + project.getName(), type);
        RunnerAndConfigurationSettings settings =
                runManager.createConfiguration(configurationName, factory);
        ApplicationConfiguration configuration =
                (ApplicationConfiguration) settings.getConfiguration();
        configuration.setMainClassName(MAIN_CLASS_NAME);
        configuration.getConfigurationModule().setModule(module);
        if (project.getBasePath() != null) {
            configuration.setWorkingDirectory(project.getBasePath());
        }

        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        return settings;
    }

    static boolean matches(ApplicationConfiguration configuration, Module module) {
        return MAIN_CLASS_NAME.equals(configuration.getMainClassName())
                && Objects.equals(configuration.getConfigurationModule().getModule(), module);
    }

    @Override
    public void dispose() {
        // Disposed automatically by the platform when the project closes; nothing to release here
        // beyond expiring any in-flight read action via expireWith(this).
    }
}
