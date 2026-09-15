package org.ikasan.studio.intellij.project;

import com.intellij.find.FindModel;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.TestApplicationManager;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioReplaceInFilesTest {
    @BeforeAll
    static void initializeApplication() {
        // ReplaceInProjectManager initializes platform notification groups before Mockito can mock it.
        TestApplicationManager.getInstance();
    }

    @Test void waitsForSuccessAndNonModalUiThenOpensPrefilledProjectReplacement() {
        Project project = mock(Project.class);
        Module module = mock(Module.class);
        UiContext context = mock(UiContext.class);
        ReplaceInProjectManager manager = mock(ReplaceInProjectManager.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        when(project.getService(ReplaceInProjectManager.class)).thenReturn(manager);
        Application application = mock(Application.class);
        try (var applications = mockStatic(ApplicationManager.class)) {
            // Mockito records this invocation to configure the static mock.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            CompletableFuture<Void> generation = new CompletableFuture<>();
            StudioReplaceInFiles.afterGeneration(project, module, generation, "org.example.cat.domain", "org.example.debug.domain");
            verifyNoInteractions(application, manager);
            generation.complete(null);
            var callback = ArgumentCaptor.forClass(Runnable.class);
            verify(application).invokeLater(callback.capture(), eq(ModalityState.nonModal()));
            verifyNoInteractions(manager);
            callback.getValue().run();
            var model = ArgumentCaptor.forClass(FindModel.class);
            var data = ArgumentCaptor.forClass(DataContext.class);
            verify(manager).replaceInProject(data.capture(), model.capture());
            assertSame(DataContext.EMPTY_CONTEXT, data.getValue());
            FindModel find = model.getValue();
            assertEquals("org.example.cat.domain", find.getStringToFind());
            assertEquals("org.example.debug.domain", find.getStringToReplace());
            assertTrue(find.isReplaceState());
            assertTrue(find.isMultipleFiles());
            assertTrue(find.isProjectScope());
            assertTrue(find.isCaseSensitive());
            assertFalse(find.isRegularExpressions());
            assertFalse(find.isReplaceAll());
            assertTrue(find.isPromptOnReplace());
            assertNull(find.getFileFilter());
            verifyNoMoreInteractions(manager);
        }
    }

    @Test void failureCancellationDisposalAndReloadDoNotOpenAReplacementWindow() {
        Project project = mock(Project.class);
        Module module = mock(Module.class);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        Application application = mock(Application.class);
        try (var applications = mockStatic(ApplicationManager.class)) {
            // Mockito records this invocation to configure the static mock.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(application);
            StudioReplaceInFiles.afterGeneration(project, module, CompletableFuture.failedFuture(new IllegalStateException()), "old.Type", "new.Type");
            CompletableFuture<Void> canceled = new CompletableFuture<>();
            canceled.cancel(false);
            StudioReplaceInFiles.afterGeneration(project, module, canceled, "old.Type", "new.Type");
            when(project.isDisposed()).thenReturn(true);
            StudioReplaceInFiles.afterGeneration(project, module, CompletableFuture.completedFuture(null), "old.Type", "new.Type");
            verifyNoInteractions(application);

            when(project.isDisposed()).thenReturn(false);
            StudioReplaceInFiles.afterGeneration(project, module, CompletableFuture.completedFuture(null), "old.Type", "new.Type");
            var callback = ArgumentCaptor.forClass(Runnable.class);
            verify(application).invokeLater(callback.capture(), eq(ModalityState.nonModal()));
            when(context.getIkasanModule()).thenReturn(mock(Module.class));
            callback.getValue().run();
            verify(project, never()).getService(ReplaceInProjectManager.class);
        }
    }
}
