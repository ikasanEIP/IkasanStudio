package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ModelLoadAction implements ActionListener {
    private final String projectKey;

    public ModelLoadAction(String projectKey) {
        this.projectKey = projectKey;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        StudioUIUtils.displayIdeaInfoMessage(projectKey, "Load json model from file.");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            StudioPsiUtils.synchGenerateModelInstanceFromJSON(projectKey);
            if (UiContext.getPalettePanel(projectKey) != null) {
                UiContext.getPalettePanel(projectKey).resetPallette();
            }
            UiContext.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
            UiContext.getDesignerCanvas(projectKey).repaint();
            UiContext.getPalettePanel(projectKey).repaint();
        });
    }
}
