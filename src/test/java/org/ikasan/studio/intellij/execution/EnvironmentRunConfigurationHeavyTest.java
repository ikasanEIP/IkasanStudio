package org.ikasan.studio.intellij.execution;

import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.execution.RunnerAndConfigurationSettings;

public class EnvironmentRunConfigurationHeavyTest extends HeavyPlatformTestCase {
    public void testEnvironmentsAndDebugHaveIndependentReusableLocalConfigurations() {
        IkasanRunConfigurationService service = getProject().getService(IkasanRunConfigurationService.class);
        RunnerAndConfigurationSettings dev = service.findOrCreateEnvironmentConfiguration(myModule, false, "dev");
        RunnerAndConfigurationSettings debug = service.findOrCreateEnvironmentConfiguration(myModule, true, "dev");
        RunnerAndConfigurationSettings integration = service.findOrCreateEnvironmentConfiguration(myModule, false, "int");
        RunnerAndConfigurationSettings shared = service.findOrCreateEnvironmentConfiguration(myModule, false, "");
        try {
            assertNotSame(dev, debug);
            assertNotSame(dev, integration);
            assertNotSame(dev, shared);
            assertSame(dev, service.findOrCreateEnvironmentConfiguration(myModule, false, "dev"));
            assertSame(debug, service.findOrCreateEnvironmentConfiguration(myModule, true, "dev"));
            ApplicationConfiguration app = (ApplicationConfiguration) dev.getConfiguration();
            assertEquals(IkasanRunConfigurationService.MAIN_CLASS_NAME, app.getMainClassName());
            assertSame(myModule, app.getConfigurationModule().getModule());
            assertEquals(getProject().getBasePath(), app.getWorkingDirectory());
            assertFalse(dev.isShared());
            assertFalse(debug.isShared());
            String runParameters = app.getProgramParameters();
            String debugParameters = ((ApplicationConfiguration) debug.getConfiguration()).getProgramParameters();
            assertNotNull(runParameters);
            assertNotNull(debugParameters);
            assertFalse(runParameters.contains("studio-debug"));
            assertTrue(debugParameters.contains("dev,studio-debug"));
            assertFalse(IkasanRunConfigurationService.matches(app, myModule, false));
        } finally {
            RunManager manager = RunManager.getInstance(getProject());
            for (RunnerAndConfigurationSettings settings : java.util.List.of(dev, debug, integration, shared)) manager.removeConfiguration(settings);
        }
    }
}
