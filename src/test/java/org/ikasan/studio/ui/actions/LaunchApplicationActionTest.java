package org.ikasan.studio.ui.actions;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LaunchApplicationActionTest {
    @Test
    void failedGenerationNeverLaunchesAndReportsOnce() {
        var generation = new CompletableFuture<Void>();
        var launched = new AtomicBoolean();
        var failures = new AtomicInteger();
        LaunchApplicationAction.continueAfterGeneration(generation, () -> false,
                () -> launched.set(true), failure -> failures.incrementAndGet(), Runnable::run);
        generation.completeExceptionally(new java.io.IOException("Dependency resolution failed while offline"));
        assertFalse(launched.get());
        assertEquals(1, failures.get());
    }

    @Test
    void projectClosedAfterCompletionButBeforeUiCallbackNeverLaunches() {
        var callbacks = new java.util.ArrayList<Runnable>();
        var disposed = new AtomicBoolean();
        LaunchApplicationAction.continueAfterGeneration(CompletableFuture.completedFuture(null), disposed::get,
                () -> fail("Launched after project closed"), failure -> fail("Reported after project closed"), callbacks::add);
        disposed.set(true);
        callbacks.forEach(Runnable::run);
    }
}
