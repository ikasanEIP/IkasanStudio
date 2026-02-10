package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelLoadAction implements ActionListener {
    private final Project project;

    public ModelLoadAction(Project project) {
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        StudioUIUtils.displayIdeaInfoMessage(project, "Load json model from file.");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            StudioPsiUtils.synchGenerateModelInstanceFromJSON(project);
            // All UI operations must run on EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                UiContext uiContext = project.getService(UiContext.class);
                if (uiContext.getPalettePanel() != null) {
                    uiContext.getPalettePanel().resetPallette();
                }
                uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
                uiContext.getDesignerCanvas().repaint();
                uiContext.getPalettePanel().repaint();
            });
        });
    }
}
