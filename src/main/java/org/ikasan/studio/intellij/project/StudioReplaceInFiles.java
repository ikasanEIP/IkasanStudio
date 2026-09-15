package org.ikasan.studio.intellij.project;

import com.intellij.find.FindModel;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.util.concurrent.CompletableFuture;

/** Opens IntelliJ's review UI; never executes a file replacement on the user's behalf. */
public final class StudioReplaceInFiles {
    private static final Logger LOG = Logger.getInstance(StudioReplaceInFiles.class);
    private StudioReplaceInFiles() { }

    public static void afterGeneration(Project project, Module module, CompletableFuture<Void> generation,
                                       String find, String replacement) {
        generation.thenRun(() -> {
            if (project.isDisposed()) return;
            // Wait for the Studio dialog (and any other modal UI) to close before opening IntelliJ's popup.
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed() || project.getService(UiContext.class).getIkasanModule() != module) return;
                try {
                    // The manager owns the project and the explicit model owns the scope; no editor context is needed.
                    ReplaceInProjectManager.getInstance(project).replaceInProject(DataContext.EMPTY_CONTEXT, model(find, replacement));
                } catch (RuntimeException failure) {
                    LOG.warn("STUDIO: Could not open Replace in Files", failure);
                    StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ReplaceReferencesOpenFilesFailed"));
                }
            }, ModalityState.nonModal());
        });
    }

    static FindModel model(String find, String replacement) {
        // Start fresh so a previous file mask, directory scope or regex setting cannot hide/change matches.
        FindModel model = new FindModel();
        model.setStringToFind(find);
        model.setStringToReplace(replacement);
        model.setReplaceState(true);
        model.setMultipleFiles(true);
        model.setProjectScope(true);
        model.setCaseSensitive(true);
        model.setRegularExpressions(false);
        model.setWholeWordsOnly(false);
        model.setReplaceAll(false);
        model.setPromptOnReplace(true);
        model.setSearchContext(FindModel.SearchContext.ANY);
        return model;
    }
}
