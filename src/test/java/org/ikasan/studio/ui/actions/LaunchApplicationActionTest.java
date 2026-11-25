package org.ikasan.studio.ui.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaunchApplicationActionTest {
    @Test
    void failedGenerationNeverLaunchesAndReportsOnce() {
        CompletableFuture<Void> generation = new CompletableFuture<>();
        AtomicBoolean launched = new AtomicBoolean();
        AtomicInteger failures = new AtomicInteger();
        LaunchApplicationAction.continueAfterGeneration(generation, () -> false,
                () -> launched.set(true), failure -> failures.incrementAndGet(), Runnable::run);
        generation.completeExceptionally(new IOException("Dependency resolution failed while offline"));
        assertFalse(launched.get());
        assertEquals(1, failures.get());
    }

    @Test
    void projectClosedAfterCompletionButBeforeUiCallbackNeverLaunches() {
        ArrayList<Runnable> callbacks = new ArrayList<>();
        AtomicBoolean disposed = new AtomicBoolean();
        LaunchApplicationAction.continueAfterGeneration(CompletableFuture.completedFuture(null), disposed::get,
                () -> fail("Launched after project closed"), failure -> fail("Reported after project closed"), callbacks::add);
        disposed.set(true);
        callbacks.forEach(Runnable::run);
    }
}
