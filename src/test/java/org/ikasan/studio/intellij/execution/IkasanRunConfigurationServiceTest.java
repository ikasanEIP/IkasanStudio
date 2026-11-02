package org.ikasan.studio.intellij.execution;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.openapi.module.Module;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IkasanRunConfigurationServiceTest {

    @Test
    void environmentLaunchArgumentsRoundTripWithSpacesAndKeepDebugProfile() {
        String run = IkasanRunConfigurationService.environmentParameters("/tmp/my project", "dev", false);
        String debug = IkasanRunConfigurationService.environmentParameters("/tmp/my project", "dev", true);
        assertThat(com.intellij.util.execution.ParametersListUtil.parse(run)).containsExactly(
                "--spring.config.additional-location=optional:file:/tmp/my project/config/,optional:file:/tmp/my project/config/local/",
                "--spring.profiles.active=dev");
        assertThat(com.intellij.util.execution.ParametersListUtil.parse(debug)).contains("--spring.profiles.active=dev,studio-debug");
        assertThat(IkasanRunConfigurationService.environmentOf(run)).isEqualTo("dev");
        assertThat(IkasanRunConfigurationService.environmentOf(debug)).isEqualTo("dev");
        assertThat(IkasanRunConfigurationService.environmentOf(
                IkasanRunConfigurationService.environmentParameters("/tmp/project", "", true))).isEmpty();
        assertThat(IkasanRunConfigurationService.environmentOf(IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS)).isNull();
    }

    @Test
    void defaultLaunchDoesNotAccidentallyReuseAnEnvironmentConfiguration() {
        Module module = mock(Module.class);
        JavaRunConfigurationModule configurationModule = mock(JavaRunConfigurationModule.class);
        ApplicationConfiguration configuration = mock(ApplicationConfiguration.class);
        when(configuration.getMainClassName()).thenReturn(IkasanRunConfigurationService.MAIN_CLASS_NAME);
        when(configuration.getConfigurationModule()).thenReturn(configurationModule);
        when(configurationModule.getModule()).thenReturn(module);
        when(configuration.getProgramParameters()).thenReturn(
                IkasanRunConfigurationService.environmentParameters("/tmp/project", "int", false));
        assertThat(IkasanRunConfigurationService.matches(configuration, module, false)).isFalse();
        assertThat(IkasanRunConfigurationService.matches(configuration, module, true)).isFalse();
    }

    @Test
    void matchesOnlyTheGeneratedApplicationInTheSameIntellijModule() {
        Module targetModule = mock(Module.class);
        Module otherModule = mock(Module.class);
        JavaRunConfigurationModule configurationModule = mock(JavaRunConfigurationModule.class);
        ApplicationConfiguration configuration = mock(ApplicationConfiguration.class);

        when(configuration.getMainClassName())
                .thenReturn(IkasanRunConfigurationService.MAIN_CLASS_NAME);
        when(configuration.getConfigurationModule()).thenReturn(configurationModule);
        when(configurationModule.getModule()).thenReturn(targetModule);

        assertThat(IkasanRunConfigurationService.matches(configuration, targetModule, false)).isTrue();
        assertThat(IkasanRunConfigurationService.matches(configuration, otherModule, false)).isFalse();
        assertThat(IkasanRunConfigurationService.matches(configuration, targetModule, true)).isFalse();
    }

    @Test
    void doesNotReuseAnUnrelatedApplicationConfiguration() {
        Module module = mock(Module.class);
        JavaRunConfigurationModule configurationModule = mock(JavaRunConfigurationModule.class);
        ApplicationConfiguration configuration = mock(ApplicationConfiguration.class);

        when(configuration.getMainClassName()).thenReturn("org.example.OtherApplication");
        when(configuration.getConfigurationModule()).thenReturn(configurationModule);
        when(configurationModule.getModule()).thenReturn(module);

        assertThat(IkasanRunConfigurationService.matches(configuration, module, false)).isFalse();
    }

    @Test
    void matchesTheDebugVariantOnlyWhenItsProgramParametersActivateTheStudioDebugProfile() {
        Module module = mock(Module.class);
        JavaRunConfigurationModule configurationModule = mock(JavaRunConfigurationModule.class);
        ApplicationConfiguration configuration = mock(ApplicationConfiguration.class);

        when(configuration.getMainClassName()).thenReturn(IkasanRunConfigurationService.MAIN_CLASS_NAME);
        when(configuration.getConfigurationModule()).thenReturn(configurationModule);
        when(configurationModule.getModule()).thenReturn(module);
        when(configuration.getProgramParameters())
                .thenReturn(IkasanRunConfigurationService.STUDIO_DEBUG_PROGRAM_PARAMETERS);

        assertThat(IkasanRunConfigurationService.matches(configuration, module, true)).isTrue();
        assertThat(IkasanRunConfigurationService.matches(configuration, module, false)).isFalse();
    }
}
