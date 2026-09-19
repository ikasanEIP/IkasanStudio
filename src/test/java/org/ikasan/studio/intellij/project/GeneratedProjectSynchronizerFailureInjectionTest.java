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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
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
        var lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        var application = mock(Application.class);
        when(application.getDefaultModalityState()).thenReturn(mock(com.intellij.openapi.application.ModalityState.class));
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
            assertEquals(2, queued.size());
            assertFalse(context.tryBeginMigration());
            queued.get(0).run(); // Superseded request waits for its replacement.
            assertFalse(first.isDone());
            when(project.isDisposed()).thenReturn(true);
            // A mock Project does not dispose its services: close the actual cancellation parent explicitly.
            Disposer.dispose(lifetime);
            assertThrows(CancellationException.class, first::join);
            assertThrows(CancellationException.class, second::join);
            assertTrue(context.tryBeginMigration());
            context.endMigration();
            queued.get(1).run(); // An already-queued callback must not touch the closed project.
            assertTrue(context.tryBeginMigration()); // Late callbacks must not release the guard twice.
            context.endMigration();
            verify(project, never()).getBasePath();
        } finally { Disposer.dispose(lifetime); }
    }

    @Test
    void rejectedBackgroundTaskCompletesFailureAndReleasesGuard() {
        var project = mock(Project.class);
        var context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        var lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        var application = mock(Application.class);
        when(application.getDefaultModalityState()).thenReturn(mock(com.intellij.openapi.application.ModalityState.class));
        var rejection = new RejectedExecutionException("Executor shutting down");
        when(application.executeOnPooledThread(any(Runnable.class))).thenThrow(rejection);
        try (var applications = mockStatic(ApplicationManager.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            var completion = new GeneratedProjectSynchronizer(project)
                    .asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            assertSame(rejection, assertThrows(CompletionException.class, completion::join).getCause());
            assertTrue(context.tryBeginMigration());
            context.endMigration();
        } finally { Disposer.dispose(lifetime); }
    }

    @Test
    void generationCommitRetainsInitiatingModalityAcrossBackgroundWork() {
        var project = mock(Project.class);
        var context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        var lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        var application = mock(Application.class);
        var initiatingModality = mock(com.intellij.openapi.application.ModalityState.class);
        var pooledModality = mock(com.intellij.openapi.application.ModalityState.class);
        var background = new ArrayList<Runnable>();
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(call -> {
            background.add(call.getArgument(0));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });
        try (var applications = mockStatic(ApplicationManager.class);
             var modalities = mockStatic(com.intellij.openapi.application.ModalityState.class);
             var files = mockStatic(StudioProjectFiles.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            modalities.when(com.intellij.openapi.application.ModalityState::defaultModalityState).thenReturn(initiatingModality);
            var completion = new GeneratedProjectSynchronizer(project)
                    .asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            modalities.when(com.intellij.openapi.application.ModalityState::defaultModalityState).thenReturn(pooledModality);
            assertEquals(1, background.size());
            background.get(0).run();
            files.verify(() -> StudioProjectFiles.pomLoadFromVirtualDisk(project));
            var commit = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(application).invokeLater(commit.capture(), same(initiatingModality));
            verify(application, never()).invokeLater(any(Runnable.class));
            assertFalse(completion.isDone());
            // The modality-delayed callback must still honour project closure before writing anything.
            when(project.isDisposed()).thenReturn(true);
            commit.getValue().run();
            assertThrows(CancellationException.class, completion::join);
            files.verifyNoMoreInteractions();
            assertTrue(context.tryBeginMigration());
            context.endMigration();
        } finally { Disposer.dispose(lifetime); }
    }

}
