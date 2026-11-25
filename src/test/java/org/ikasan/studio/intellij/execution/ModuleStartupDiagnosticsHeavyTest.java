package org.ikasan.studio.intellij.execution;

import com.intellij.execution.process.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import java.nio.file.Path;

public class ModuleStartupDiagnosticsHeavyTest extends HeavyPlatformTestCase {
    public void testFailureIsVisibleWhileJvmRemainsAliveAndNewLaunchClearsIt() {
        ModuleStartupDiagnostics service = new ModuleStartupDiagnostics(getProject());
        try {
            ProcessHandler first = mock(ProcessHandler.class);
            ProcessListener listener = attach(service, first);
            listener.onTextAvailable(new ProcessEvent(first,
                    "APPLICATION FAILED TO START\nWeb server failed to start. Port 12450 was already in use."),
                    ProcessOutputTypes.STDERR);
            String message = service.message();
            assertNotNull(message);
            assertTrue(message.contains("12450"));
            verify(first, never()).destroyProcess();
            ProcessHandler second = mock(ProcessHandler.class);
            attach(service, second);
            assertNull(service.message());
            listener.onTextAvailable(new ProcessEvent(first, "APPLICATION FAILED TO START"), ProcessOutputTypes.STDERR);
            assertNull(service.message());
        } finally { Disposer.dispose(service); }
    }

    public void testRegisteredApplicationExtensionAttachesProjectStartupObserver() {
        ApplicationConfiguration configuration = new com.intellij.execution.application.ApplicationConfiguration(
                "Studio startup regression", getProject(), com.intellij.execution.application.ApplicationConfigurationType.getInstance());
        configuration.setMainClassName("org.ikasan.studio.boot.Application");
        ProcessHandler handler = mock(ProcessHandler.class);
        com.intellij.execution.JavaRunConfigurationExtensionManager.getInstance()
                .attachExtensionsToProcess(configuration, handler, null);
        ArgumentCaptor<ProcessListener> listener = ArgumentCaptor.forClass(ProcessListener.class);
        verify(handler, atLeastOnce()).addProcessListener(listener.capture());
        for (ProcessListener registered : listener.getAllValues()) registered.onTextAvailable(new ProcessEvent(handler,
                "APPLICATION FAILED TO START\nWeb server failed to start. Port 12450 was already in use."),
                new ProcessOutputType("studio.startup.red", ProcessOutputType.STDOUT));
        assertNotNull(getProject().getService(ModuleStartupDiagnostics.class).message());
    }

    public void testFailureIsDetectedInIntellijColouredOutput() {
        for (ProcessOutputType base : new ProcessOutputType[]{ProcessOutputType.STDOUT, ProcessOutputType.STDERR}) {
            ModuleStartupDiagnostics service = new ModuleStartupDiagnostics(getProject());
            try {
                ProcessHandler handler = mock(ProcessHandler.class);
                ProcessListener listener = attach(service, handler);
                ProcessOutputType coloured = new ProcessOutputType("studio.startup.red", base);
                org.junit.Assert.assertNotEquals(base, coloured);
                listener.onTextAvailable(new ProcessEvent(handler,
                        "APPLICATION FAILED TO START\nWeb server failed to start. Port 12450 was already in use."), coloured);
                String message = service.message();
                assertNotNull("Coloured " + base + " must reach startup detection", message);
                assertTrue(message.contains("12450"));
            } finally { Disposer.dispose(service); }
        }
    }

    public void testRealColouredProcessReportsFailureEvenWithZeroExitCode() throws Exception {
        Path directory = java.nio.file.Files.createTempDirectory("studio-coloured-startup");
        Path source = directory.resolve("StartupFailureFixture.java");
        java.nio.file.Files.writeString(source, """
                public class StartupFailureFixture {
                    public static void main(String[] args) {
                        System.out.println((char) 27 + "[31mAPPLICATION FAILED TO START" + (char) 27 + "[0m");
                        System.out.println((char) 27 + "[31mWeb server failed to start. Port 12450 was already in use." + (char) 27 + "[0m");
                    }
                }
                """);
        ModuleStartupDiagnostics service = new ModuleStartupDiagnostics(getProject());
        java.util.concurrent.Future<Boolean> task = null;
        try {
            task = com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(
                    () -> {
                        String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
                        GeneralCommandLine command = new com.intellij.execution.configurations.GeneralCommandLine(executable, source.toString())
                                .withWorkDirectory(directory.toFile());
                        ColoredProcessHandler handler = new ColoredProcessHandler(command);
                        try {
                            service.attach(handler);
                            handler.startNotify();
                            if (!handler.waitFor(15_000)) throw new AssertionError("Startup fixture did not terminate");
                            if (!Integer.valueOf(0).equals(handler.getExitCode()))
                                throw new AssertionError("Startup fixture failed to run");
                            String message = service.message();
                            return message != null && message.contains("12450");
                        } finally {
                            if (!handler.isProcessTerminated()) handler.destroyProcess();
                        }
                    });
            assertTrue("Real ANSI-coloured process output must produce the port-conflict diagnostic",
                    task.get(20, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            if (task != null && !task.isDone()) task.cancel(true);
            Disposer.dispose(service);
            com.intellij.openapi.util.io.FileUtil.delete(directory.toFile());
        }
    }

    public void testPrematureShutdownExplainsEvidenceAndUnknownCause() {
        ModuleStartupDiagnostics service = new ModuleStartupDiagnostics(getProject());
        try {
            ProcessHandler handler = mock(ProcessHandler.class);
            ProcessListener listener = attach(service, handler);
            listener.onTextAvailable(new ProcessEvent(handler,
                    "o.i.m.s.ModuleInitialisationServiceImpl : Shutting down Quartz scheduler with bean name: scheduler"),
                    ProcessOutputTypes.STDOUT);
            assertTrue(service.bannerMessage().contains("before startup completed"));
            assertTrue(service.message().contains("No cause was reported"));
            assertFalse(service.message().contains("12450"));
            verify(handler, never()).destroyProcess();
        } finally { Disposer.dispose(service); }
    }

    public void testExplicitOperatorStopDoesNotReportStartupFailure() {
        ModuleStartupDiagnostics service = new ModuleStartupDiagnostics(getProject());
        try {
            ProcessHandler handler = mock(ProcessHandler.class);
            ProcessListener listener = attach(service, handler);
            listener.processWillTerminate(new ProcessEvent(handler), true);
            listener.onTextAvailable(new ProcessEvent(handler,
                    "o.i.m.s.ModuleInitialisationServiceImpl : Shutting down Quartz scheduler with bean name: scheduler"),
                    ProcessOutputTypes.STDOUT);
            listener.processTerminated(new ProcessEvent(handler, 137));
            assertNull(service.message());
        } finally { Disposer.dispose(service); }
    }

    private ProcessListener attach(ModuleStartupDiagnostics service, ProcessHandler handler) {
        service.attach(handler);
        ArgumentCaptor<ProcessListener> listener = ArgumentCaptor.forClass(ProcessListener.class);
        verify(handler).addProcessListener(listener.capture());
        return listener.getValue();
    }
}
