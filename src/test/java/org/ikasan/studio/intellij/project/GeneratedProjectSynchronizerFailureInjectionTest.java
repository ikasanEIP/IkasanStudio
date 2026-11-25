package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// ApplicationManager::getApplication as a MockedStatic#when(Verification) argument is Mockito's own static-mocking
// idiom, not a discarded real call - the IDE's "ignored result" inspection doesn't recognise that shape.
@SuppressWarnings("ResultOfMethodCallIgnored")
class GeneratedProjectSynchronizerFailureInjectionTest {
    @Test
    void overlappingRequestsAreCancelledOnProjectCloseAndReleaseMigrationGuard() {
        Project project = mock(Project.class);
        UiContext context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        // GeneratedProjectSynchronizer registers its per-request cancellation lifetime against this service
        // rather than the Project itself (see its own comment) - a real project always has one, but this mock
        // needs it stubbed explicitly or Disposer.register(null, ...) fails before generation ever begins.
        StudioProjectInitialisationService lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        Application application = mock(Application.class);
        when(application.getDefaultModalityState()).thenReturn(mock(ModalityState.class));
        ArrayList<Runnable> queued = new ArrayList<>();
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(call -> {
            queued.add(call.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        try (MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            GeneratedProjectSynchronizer synchronizer = new GeneratedProjectSynchronizer(project);
            CompletableFuture<Void> first = synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            CompletableFuture<Void> second = synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.properties());
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
        Project project = mock(Project.class);
        UiContext context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        StudioProjectInitialisationService lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        Application application = mock(Application.class);
        when(application.getDefaultModalityState()).thenReturn(mock(ModalityState.class));
        RejectedExecutionException rejection = new RejectedExecutionException("Executor shutting down");
        when(application.executeOnPooledThread(any(Runnable.class))).thenThrow(rejection);
        try (MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            CompletableFuture<Void> completion = new GeneratedProjectSynchronizer(project)
                    .asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            assertSame(rejection, assertThrows(CompletionException.class, completion::join).getCause());
            assertTrue(context.tryBeginMigration());
            context.endMigration();
        } finally { Disposer.dispose(lifetime); }
    }

    @Test
    void generationCommitRetainsInitiatingModalityAcrossBackgroundWork() {
        Project project = mock(Project.class);
        UiContext context = new UiContext();
        context.setIkasanModule(mock(Module.class));
        when(project.getService(UiContext.class)).thenReturn(context);
        StudioProjectInitialisationService lifetime = mock(StudioProjectInitialisationService.class);
        when(project.getService(StudioProjectInitialisationService.class)).thenReturn(lifetime);
        Application application = mock(Application.class);
        ModalityState initiatingModality = mock(ModalityState.class);
        ModalityState pooledModality = mock(ModalityState.class);
        ArrayList<Runnable> background = new ArrayList<>();
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(call -> {
            background.add(call.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        try (MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class);
             MockedStatic<ModalityState> modalities = mockStatic(ModalityState.class);
             MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class)) {
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            modalities.when(ModalityState::defaultModalityState).thenReturn(initiatingModality);
            CompletableFuture<Void> completion = new GeneratedProjectSynchronizer(project)
                    .asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest.full());
            modalities.when(ModalityState::defaultModalityState).thenReturn(pooledModality);
            assertEquals(1, background.size());
            background.get(0).run();
            files.verify(() -> StudioProjectFiles.pomLoadFromVirtualDisk(project));
            ArgumentCaptor<Runnable> commit = ArgumentCaptor.forClass(Runnable.class);
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
