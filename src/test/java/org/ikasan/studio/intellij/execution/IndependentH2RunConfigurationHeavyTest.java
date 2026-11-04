package org.ikasan.studio.intellij.execution;

import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.ide.BrowserUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ui.UIUtil;
import org.ikasan.studio.ui.actions.LaunchH2Action;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IndependentH2RunConfigurationHeavyTest extends HeavyPlatformTestCase {
    public void testConfigurationUsesProjectModuleAndIsReusedWithUpdatedPorts() {
        var service = getProject().getService(IndependentH2RunConfigurationService.class);
        var first = service.findOrCreate(myModule, "12451 12452");
        try {
            var app = (ApplicationConfiguration) first.getConfiguration();
            assertSame(first, service.findOrCreate(myModule, "12551 12552"));
            assertEquals(IndependentH2RunConfigurationService.MAIN_CLASS, app.getMainClassName());
            assertSame(myModule, app.getConfigurationModule().getModule());
            assertEquals(getProject().getBasePath(), app.getWorkingDirectory());
            assertEquals("12551 12552", app.getProgramParameters());
            assertFalse(first.isShared());
            assertFalse(IndependentH2RunConfigurationService.isOurs(new Object()));
        } finally { RunManager.getInstance(getProject()).removeConfiguration(first); }
    }

    public void testActualProcessStateSurvivesMenuRecreationAndGracefulStop() throws Exception {
        var service = getProject().getService(IndependentH2RunConfigurationService.class);
        var handler = new FakeProcess();
        var button = new JButton();
        new LaunchH2Action(getProject(), button).listen(getTestRootDisposable());
        try (var browser = org.mockito.Mockito.mockStatic(BrowserUtil.class)) {
            service.attach(handler); service.attach(handler); handler.startNotify();
            assertEquals(IndependentH2RunConfigurationService.State.STARTING, service.state());
            handler.text("IKASAN_STUDIO_H2_RE");
            assertEquals(IndependentH2RunConfigurationService.State.STARTING, service.state());
            handler.text("ADY tcp=12451 web=12");
            assertEquals(IndependentH2RunConfigurationService.State.STARTING, service.state());
            handler.text("452\n");
            UIUtil.dispatchAllInvocationEvents();
            assertEquals(IndependentH2RunConfigurationService.State.RUNNING, service.state());
            browser.verify(() -> BrowserUtil.browse("http://127.0.0.1:12452/"));
            var menu = new JMenuItem(); new LaunchH2Action(getProject(), menu);
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("h2.independent.stop"), menu.getText());
            assertTrue(menu.isEnabled());
            service.requestStop();
            assertTrue(handler.written.await(2, TimeUnit.SECONDS));
            assertEquals("STOP\n", handler.input.toString(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(IndependentH2RunConfigurationService.State.STOPPING, service.state());
            handler.finish(); UIUtil.dispatchAllInvocationEvents();
            assertEquals(IndependentH2RunConfigurationService.State.STOPPED, service.state());
            assertEquals(org.ikasan.studio.ui.StudioBundle.message("button.H2Start"), button.getText());
        } finally { if (!handler.isProcessTerminated()) handler.finish(); }
    }

    public void testNormalIdeStopResetsStateAndOnlyH2RunsAreTracked() throws Exception {
        var service = getProject().getService(IndependentH2RunConfigurationService.class);
        var settings = service.findOrCreate(myModule, "12451 12452");
        try {
            var extension = new IndependentH2RunExtension();
            var config = (ApplicationConfiguration) settings.getConfiguration();
            var handler = new FakeProcess();
            extension.attachToProcess(config, handler, null); handler.startNotify();
            assertEquals(IndependentH2RunConfigurationService.State.STARTING, service.state());
            handler.destroyProcess(); UIUtil.dispatchAllInvocationEvents();
            assertEquals(IndependentH2RunConfigurationService.State.STOPPED, service.state());
            config.setMainClassName(IkasanRunConfigurationService.MAIN_CLASS_NAME);
            var unrelated = new FakeProcess();
            extension.attachToProcess(config, unrelated, null);
            assertEquals(IndependentH2RunConfigurationService.State.STOPPED, service.state());
        } finally { RunManager.getInstance(getProject()).removeConfiguration(settings); }
    }

    public void testClasspathDiagnosticsAndPortValidation() throws Exception {
        var service = getProject().getService(IndependentH2RunConfigurationService.class);
        var settings = service.findOrCreate(myModule, "12451 12452");
        try {
            var extension = new IndependentH2RunExtension();
            var config = (ApplicationConfiguration) settings.getConfiguration();
            var parameters = new JavaParameters();
            try { extension.updateJavaParameters(config, parameters, null); fail("Missing H2 must fail before launch"); }
            catch (com.intellij.execution.ExecutionException expected) { assertTrue(expected.getMessage().contains("H2")); }
            parameters.getClassPath().add("/repository/h2-mvstore-2.4.240.jar");
            try { extension.updateJavaParameters(config, parameters, null); fail("MVStore alone is not an H2 server"); }
            catch (com.intellij.execution.ExecutionException expected) { assertTrue(expected.getMessage().contains("H2")); }
            parameters.getClassPath().add("/repository/h2-2.4.240.jar");
            extension.updateJavaParameters(config, parameters, null);
            parameters.getClassPath().add("/other/h2-1.4.200.jar");
            try { extension.updateJavaParameters(config, parameters, null); fail("Conflicting H2 versions must fail before launch"); }
            catch (com.intellij.execution.ExecutionException expected) { assertTrue(expected.getMessage().contains("Multiple H2")); }
            for (String value : new String[]{"0", "65536", "abc", "12451; command"}) {
                try { IndependentH2RunConfigurationService.parameters(value, "12452"); fail("Invalid port accepted"); }
                catch (IllegalArgumentException expected) { }
            }
            try { IndependentH2RunConfigurationService.parameters("12451", "12451"); fail("Identical ports accepted"); }
            catch (IllegalArgumentException expected) { }
        } finally { RunManager.getInstance(getProject()).removeConfiguration(settings); }
    }

    private static final class FakeProcess extends ProcessHandler {
        final CountDownLatch written = new CountDownLatch(1);
        final ByteArrayOutputStream input = new ByteArrayOutputStream() {
            @Override public void flush() { written.countDown(); }
        };
        void text(String text) { notifyTextAvailable(text, ProcessOutputTypes.STDOUT); }
        void finish() { notifyProcessTerminated(0); }
        @Override protected void destroyProcessImpl() { finish(); }
        @Override protected void detachProcessImpl() { notifyProcessDetached(); }
        @Override public boolean detachIsDefault() { return false; }
        @Override public OutputStream getProcessInput() { return input; }
    }
}
