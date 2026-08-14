package org.ikasan.studio.ui.intellij;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IkasanDebugSessionServiceTest {

    @Test
    void recognisesOnlyStudioApplicationWithDebugExecutorAndProfile() {
        ExecutionEnvironment environment = environment(
                IkasanRunConfigurationService.MAIN_CLASS_NAME,
                IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS);

        assertThat(IkasanDebugSessionService.isStudioDebugExecution(
                DefaultDebugExecutor.EXECUTOR_ID, environment)).isTrue();
        assertThat(IkasanDebugSessionService.isStudioDebugExecution(
                DefaultRunExecutor.EXECUTOR_ID, environment)).isFalse();
    }

    @Test
    void recognisesBothRunAndDebugConfigurationsAsStudioModuleExecutions() {
        ExecutionEnvironment runEnvironment = environment(
                IkasanRunConfigurationService.MAIN_CLASS_NAME, null);
        ExecutionEnvironment debugEnvironment = environment(
                IkasanRunConfigurationService.MAIN_CLASS_NAME,
                IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS);

        assertThat(IkasanDebugSessionService.isStudioModuleExecution(runEnvironment)).isTrue();
        assertThat(IkasanDebugSessionService.isStudioModuleExecution(debugEnvironment)).isTrue();
    }

    @Test
    void rejectsDebugLaunchWithoutStudioDebugProfile() {
        ExecutionEnvironment environment = environment(
                IkasanRunConfigurationService.MAIN_CLASS_NAME, null);

        assertThat(IkasanDebugSessionService.isStudioDebugExecution(
                DefaultDebugExecutor.EXECUTOR_ID, environment)).isFalse();
    }

    @Test
    void rejectsUnrelatedApplicationUsingStudioDebugProfile() {
        ExecutionEnvironment environment = environment(
                "org.example.OtherApplication",
                IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS);

        assertThat(IkasanDebugSessionService.isStudioDebugExecution(
                DefaultDebugExecutor.EXECUTOR_ID, environment)).isFalse();
    }

    private ExecutionEnvironment environment(String mainClass, String parameters) {
        ExecutionEnvironment environment = mock(ExecutionEnvironment.class);
        RunnerAndConfigurationSettings settings = mock(RunnerAndConfigurationSettings.class);
        ApplicationConfiguration configuration = mock(ApplicationConfiguration.class);
        when(environment.getRunnerAndConfigurationSettings()).thenReturn(settings);
        when(settings.getConfiguration()).thenReturn(configuration);
        when(configuration.getMainClassName()).thenReturn(mainClass);
        when(configuration.getProgramParameters()).thenReturn(parameters);
        when(configuration.getConfigurationModule()).thenReturn(mock(
                com.intellij.execution.configurations.JavaRunConfigurationModule.class));
        when(configuration.getConfigurationModule().getModule())
                .thenReturn(mock(com.intellij.openapi.module.Module.class));
        return environment;
    }
}
