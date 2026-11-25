package org.ikasan.studio.intellij.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import org.ikasan.studio.core.generator.HarnessOverridesTemplate.LaunchTarget;
import org.ikasan.studio.core.generator.HarnessOverridesTemplate;
import org.ikasan.studio.intellij.runtime.TestFtpServerService;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SftpHarnessRunExtensionTest {
    @TempDir Path project;
    @Test void injectsOwnedFtpEndpointForSelectedLaunchesAndRejectsStoppedHarness() throws Exception {
        Project owner = Mockito.mock(Project.class);
        Mockito.when(owner.getBasePath()).thenReturn(project.toString());
        Mockito.when(owner.getService(UiContext.class))
                .thenReturn(Mockito.mock(UiContext.class));
        Mockito.when(owner.getService(HarnessOverrideService.class)).thenReturn(Mockito.mock(HarnessOverrideService.class));
        TestFtpServerService ftp = new TestFtpServerService(owner);
        Mockito.when(owner.getService(TestFtpServerService.class)).thenReturn(ftp);
        ApplicationConfiguration app = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(app.getProject()).thenReturn(owner);
        Mockito.when(app.getVMParameters()).thenReturn("-Dstudio.harness.components=flow.ftp");
        Path inventory = project.resolve(HarnessOverridesTemplate.PATH);
        Path adapter = project.resolve(HarnessOverridesTemplate.JAVA_PATH);
        Path factory = project.resolve("generated/src/main/java/org/ikasan/studio/boot/flow/orders/Factory.java");
        Files.createDirectories(inventory.getParent()); Files.createDirectories(factory.getParent()); Files.createDirectories(adapter.getParent());
        Files.writeString(inventory, """
                studio.harness.target.flow.ftp.kind=FTP
                studio.harness.target.flow.ftp.factory=org/ikasan/studio/boot/flow/orders/Factory.java
                studio.harness.target.flow.ftp.label=RlRQ
                """);
        Files.writeString(adapter, HarnessOverridesTemplate.javaSource());
        Files.writeString(factory, "StudioHarnessOverrides.apply(environment, endpoint, \"flow.ftp\", \"FTP\", false)");
        SftpHarnessRunExtension extension = new SftpHarnessRunExtension();
        try {
            assertThrows(ExecutionException.class,
                    () -> extension.updateJavaParameters(app, new JavaParameters(), null));
            ftp.startLocal();
            JavaParameters parameters = new JavaParameters();
            parameters.getVMParametersList().addProperty("studio.harness.ftp.port", "2121");
            extension.updateJavaParameters(app, parameters, null);
            assertEquals(Integer.toString(ftp.localConfiguration().port()),
                    parameters.getVMParametersList().getPropertyValue("studio.harness.ftp.port"));
            assertEquals("127.0.0.1", parameters.getVMParametersList().getPropertyValue("studio.harness.ftp.host"));
            assertEquals(ftp.getRootDirectory().toString(), parameters.getVMParametersList().getPropertyValue("studio.harness.ftp.home"));
            Mockito.when(app.getVMParameters()).thenReturn("-Dstudio.harness.components=");
            JavaParameters normal = new JavaParameters();
            extension.updateJavaParameters(app, normal, null);
            assertNull(normal.getVMParametersList().getPropertyValue("studio.harness.ftp.port"));
            assertTrue(ftp.isRunning());
            ftp.stop();
            Mockito.when(app.getVMParameters()).thenReturn("-Dstudio.harness.components=flow.ftp");
            assertThrows(ExecutionException.class,
                    () -> extension.updateJavaParameters(app, new JavaParameters(), null));
        } finally { ftp.stop(); }
    }

    @Test void refusesFactoriesWithoutSelectedComponentSupportBeforeLaunching() throws Exception {
        LaunchTarget target = new HarnessOverridesTemplate.LaunchTarget("flow.component", "SFTP",
                "org/ikasan/studio/boot/flow/orders/ComponentFactoryOrders.java", "Orders / Input");
        assertThrows(IOException.class, () ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
        Path properties = project.resolve(HarnessOverridesTemplate.PATH);
        Path adapter = project.resolve(HarnessOverridesTemplate.JAVA_PATH);
        Path factory = project.resolve("generated/src/main/java/").resolve(target.factory());
        Files.createDirectories(properties.getParent());
        Files.createDirectories(factory.getParent());
        Files.writeString(properties, "# generated harness inventory");
        Files.writeString(adapter, HarnessOverridesTemplate.javaSource());
        Files.writeString(factory, "return builderFactory.getComponentBuilder().sftpProducer().build();");
        assertThrows(IOException.class, () ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
        Files.writeString(factory, "return StudioHarnessOverrides.apply(environment, endpoint, \"flow.component\", \"SFTP\", false);");
        assertDoesNotThrow(() ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
    }
}
