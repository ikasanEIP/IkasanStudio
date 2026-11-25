package org.ikasan.studio.intellij.execution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ikasan.studio.intellij.execution.ModuleStartupOutput.Failure;

class ModuleStartupOutputTest {
    @Test void identifiesBootPortConflictAcrossChunksAndAnsiFormatting() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        assertNull(output.accept("Starting Application\nWaiting for database\n"));
        assertNull(output.accept("\u001b[31mAPPLICATION FAILED TO STA"));
        assertNotNull(output.accept("RT\u001b[0m\nDescription:\nWeb server failed to start. Po"));
        Failure failure = output.accept("rt 12450 was already in use.\nAction:\nStop the other application.");
        assertNotNull(failure);
        assertEquals(12450, failure.port());
        assertFalse(output.ready());
    }

    @Test void recognizesPortInUseExceptionWithoutFailureBanner() {
        Failure failure = new ModuleStartupOutput().accept(
                "org.springframework.boot.web.server.PortInUseException: Port 12450 is already in use");
        assertNotNull(failure);
        assertEquals(12450, failure.port());
    }

    @Test void doesNotGuessFailureFromSlowStartupOrUnrelatedBindErrors() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        assertNull(output.accept("Still importing data\njava.net.BindException: Address already in use\n"));
        assertNull(output.accept("Mail harness: Port 1025 is already in use\n"));
        assertNull(output.accept("Tomcat initialized with port 12450\n"));
        assertNull(output.accept("Started Application in 45.2 seconds\n"));
        assertTrue(output.ready());
        assertNull(output.accept("java.net.BindException: unrelated listener could not bind\n"));
    }

    @Test void recognizesFatalRunnerFailureAfterStartedMessage() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        assertNull(output.accept("Started Application in 2.5 seconds\n"));
        assertTrue(output.ready());
        assertNotNull(output.accept("ERROR o.s.boot.SpringApplication : Application run failed\n"));
        assertFalse(output.ready());
    }

    @Test void genericStartupFailureDoesNotExposeOutputOrInventAPort() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        Failure failure = output.accept("password=customer-secret\nAPPLICATION FAILED TO START\nMissing required bean.");
        assertNotNull(failure);
        assertNull(failure.port());
        assertFalse(failure.toString().contains("customer-secret"));
    }

    @Test void recognizesSpringApplicationFailureWithoutAnAnalysisBanner() {
        Failure failure = new ModuleStartupOutput().accept(
                "ERROR o.s.boot.SpringApplication : Application run failed\nIllegalStateException: startup initialization failed");
        assertNotNull(failure);
        assertNull(failure.port());
    }

    @Test void retentionIsBoundedEvenForLargeOutputEvents() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        assertNull(output.accept("x".repeat(100_000)));
        assertTrue(output.retainedCharacters() <= 8192);
        assertEquals(12450, output.accept(
                "\nWeb server failed to start. Port 12450 was already in use.").port());
    }

    @Test void detectsObservedIkasanCleanupBeforeBootCompletesWithoutGuessingCause() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        assertNull(output.accept("Tomcat initialized with port 12450 (http)\n"));
        assertNull(output.accept("Started Flow[Router Flow] in Module[routesAndBranches5]\n"));
        Failure failure = output.accept("INFO [main] o.i.m.s.ModuleInitialisationServiceImpl : "
                + "Shutting down Quartz scheduler with bean name: scheduler\n");
        assertNotNull(failure);
        assertNull(failure.port());
        assertTrue(failure.prematureShutdown());
        assertFalse(output.ready());
    }

    @Test void ordinaryShutdownAfterCompletedStartupIsNotAStartupFailure() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        output.accept("Started Application in 14.3 seconds\n");
        assertNull(output.accept("o.i.m.s.ModuleInitialisationServiceImpl : "
                + "Shutting down Quartz scheduler with bean name: scheduler\n"));
        assertTrue(output.ready());
    }

    @Test void rejectsImpossiblePortNumbers() {
        ModuleStartupOutput output = new ModuleStartupOutput();
        Failure failure = output.accept("APPLICATION FAILED TO START\nWeb server failed to start. Port 99999 was already in use.");
        assertNotNull(failure);
        assertNull(failure.port());
    }
}
