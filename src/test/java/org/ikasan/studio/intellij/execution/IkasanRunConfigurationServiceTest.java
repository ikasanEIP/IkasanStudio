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
