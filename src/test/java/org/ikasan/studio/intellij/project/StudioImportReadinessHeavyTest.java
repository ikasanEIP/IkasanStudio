package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.mockito.MockedStatic;


public class StudioImportReadinessHeavyTest extends HeavyPlatformTestCase {
    public void testMissedRootsEventIsRecoveredByReadinessRecheck() {
        AtomicInteger checks = new AtomicInteger();
        AtomicReference<StudioProjectInitialisationService> holder = new AtomicReference<>();
        StudioProjectInitialisationService service = new StudioProjectInitialisationService(myProject, () -> {
            int attempt = checks.incrementAndGet();
            if (attempt == 1) {
                // Maven publishes its final roots event while the first readiness read is still active.
                // rootsChanged delegates to start(); reproduce that callback without publishing
                // an artificial event to unrelated platform listeners.
                ApplicationManager.getApplication().invokeLater(() -> holder.get().start());
            }
            return new StudioProjectInitialisationService.ImportReadiness(attempt > 1, "waiting for generated");
        }, 20);
        holder.set(service);
        Disposer.register(getTestRootDisposable(), service);
        myProject.getMessageBus().connect(getTestRootDisposable()).subscribe(StudioProjectInitialisationService.Listener.TOPIC,
                (state, detail) -> {
                    // Stop at successful import detection; model generation is outside this regression's scope.
                    if (state == StudioProjectInitialisationService.State.WAITING_FOR_INDEXES) holder.get().dispose();
                });
        service.start();
        PlatformTestUtil.waitWithEventsDispatching("Import readiness never recovered after the missed event",
                () -> service.getState() == StudioProjectInitialisationService.State.WAITING_FOR_INDEXES, 10);
        assertTrue(checks.get() >= 2);
    }

    public void testReloadMavenRequestsAnImportAndDoesNothingAfterDisposal() {
        StudioProjectInitialisationService service = new StudioProjectInitialisationService(myProject,
                () -> new StudioProjectInitialisationService.ImportReadiness(false, "waiting"), 1000);
        Disposer.register(getTestRootDisposable(), service);
        service.start();
        PlatformTestUtil.waitWithEventsDispatching("Waiting state was not reached",
                () -> service.getState() == StudioProjectInitialisationService.State.WAITING_FOR_PROJECT_IMPORT, 10);
        MavenProjectsManager manager = org.mockito.Mockito.mock(org.jetbrains.idea.maven.project.MavenProjectsManager.class);
        try (MockedStatic<MavenProjectsManager> maven = org.mockito.Mockito.mockStatic(org.jetbrains.idea.maven.project.MavenProjectsManager.class)) {
            maven.when(() -> org.jetbrains.idea.maven.project.MavenProjectsManager.getInstance(myProject)).thenReturn(manager);
            service.reloadMavenProject();
            org.mockito.Mockito.verify(manager).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            Disposer.dispose(service);
            service.reloadMavenProject();
            org.mockito.Mockito.verifyNoMoreInteractions(manager);
        }
    }

    public void testDisposalStopsWaitingChecks() {
        AtomicInteger checks = new AtomicInteger();
        StudioProjectInitialisationService service = new StudioProjectInitialisationService(myProject, () -> {
            checks.incrementAndGet();
            return new StudioProjectInitialisationService.ImportReadiness(false, "waiting");
        }, 20);
        Disposer.register(getTestRootDisposable(), service);
        service.start();
        PlatformTestUtil.waitWithEventsDispatching("Waiting state was not reached",
                () -> service.getState() == StudioProjectInitialisationService.State.WAITING_FOR_PROJECT_IMPORT, 10);
        Disposer.dispose(service);
        int before = checks.get();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150);
        PlatformTestUtil.waitWithEventsDispatching("EDT did not finish dispatching", () -> System.nanoTime() >= deadline, 5);
        assertEquals(before, checks.get());
    }

    public void testReadinessFailureIsRecoverableInsteadOfLeavingInitializationBusy() {
        AtomicInteger checks = new AtomicInteger();
        StudioProjectInitialisationService service = new StudioProjectInitialisationService(myProject, () -> {
            if (checks.incrementAndGet() == 1) throw new IllegalStateException("injected readiness failure");
            return new StudioProjectInitialisationService.ImportReadiness(false, "waiting after retry");
        }, 1000);
        Disposer.register(getTestRootDisposable(), service);
        service.start();
        PlatformTestUtil.waitWithEventsDispatching("Readiness failure did not reach recovery",
                () -> service.getState() == StudioProjectInitialisationService.State.FAILED, 10);
        service.start();
        PlatformTestUtil.waitWithEventsDispatching("Retry remained blocked after readiness failure",
                () -> service.getState() == StudioProjectInitialisationService.State.WAITING_FOR_PROJECT_IMPORT, 10);
        Disposer.dispose(service);
    }
}
