package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelRefreshAction implements ActionListener {
    private final Project project;

    public ModelRefreshAction(Project project) {
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        UiContext uiContext = project.getService(UiContext.class);
        Module module = uiContext.getIkasanModule();

        if (module != null) {
            StudioUIUtils.displayIdeaInfoMessage(project, "Recreating json model from memory instance.");

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                StudioPsiUtils.synchGenerateModelInstanceFromJSON(project);
                // UI operations must run on EDT
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (uiContext.getPalettePanel() != null) {
                        uiContext.getPalettePanel().resetPallette();
                    }
                    uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
                    uiContext.getDesignerCanvas().repaint();
                    uiContext.getPalettePanel().repaint();
                });
            });
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project, "Refresh can't be launched unless a module is defined.");
        }
    }
}
