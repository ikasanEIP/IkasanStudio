package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// ApplicationManager::getApplication as a MockedStatic#when(Verification) argument is Mockito's own static-mocking
// idiom, not a discarded real call - the IDE's "ignored result" inspection doesn't recognise that shape.
@SuppressWarnings("ResultOfMethodCallIgnored")
class GeneratedProjectSynchronizerFailureInjectionTest {
    @Test
    void overlappingRequestsAreCancelledOnProjectCloseAndReleaseMigrationGuard() {
        var project = mock(Project.class);
        var context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        // GeneratedProjectSynchronizer registers its per-request cancellation lifetime against this service
        // rather than the Project itself (see its own comment) - a real project always has one, but this mock
        // needs it stubbed explicitly or Disposer.register(null, ...) fails before generation ever begins.
        when(project.getService(StudioProjectInitialisationService.class))
                .thenReturn(mock(StudioProjectInitialisationService.class));
        var application = mock(Application.class);
        var queued = new ArrayList<Runnable>();
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(call -> {
            queued.add(call.getArgument(0));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });
        try (var applications = mockStatic(ApplicationManager.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            var synchronizer = new GeneratedProjectSynchronizer(project);
            var first = synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            var second = synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.properties());
            assertFalse(context.tryBeginMigration());
            queued.get(0).run(); // Superseded request waits for its replacement.
            assertFalse(first.isDone());
            when(project.isDisposed()).thenReturn(true);
            Disposer.dispose(project);
            queued.get(1).run(); // An already-queued callback must not touch the closed project.
            assertTrue(first.isCompletedExceptionally());
            assertTrue(second.isCompletedExceptionally());
            assertTrue(context.tryBeginMigration());
            context.endMigration();
            verify(project, never()).getBasePath();
        }
    }

    @Test
    void rejectedBackgroundTaskCompletesFailureAndReleasesGuard() {
        var project = mock(Project.class);
        var context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        when(project.getService(StudioProjectInitialisationService.class))
                .thenReturn(mock(StudioProjectInitialisationService.class));
        var application = mock(Application.class);
        when(application.executeOnPooledThread(any(Runnable.class)))
                .thenThrow(new java.util.concurrent.RejectedExecutionException("Executor shutting down"));
        try (var applications = mockStatic(ApplicationManager.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            var completion = new GeneratedProjectSynchronizer(project)
                    .asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            assertTrue(completion.isCompletedExceptionally());
            assertTrue(context.tryBeginMigration());
            context.endMigration();
        } finally { Disposer.dispose(project); }
    }
}
