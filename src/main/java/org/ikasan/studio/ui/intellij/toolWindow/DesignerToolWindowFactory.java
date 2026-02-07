package org.ikasan.studio.ui.intellij.toolWindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.ikasan.studio.ui.DesignerUI;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;

public class DesignerToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance("#DesignerToolWindowFactory");
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        UiContext.setProject(project.getName(), project);
        if (project.isDisposed()) {
            LOG.info("STUDIO: INFO: During call to createToolWindowContent, the project has already been disposed, project will not be saved.");
            return; // Skip initialization for disposed projects
        }

        DesignerUI designerUI = new DesignerUI(toolWindow, project.getName());

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(designerUI.getContent(), "", false);
        // Get the Module metadata established before anything else.
        toolWindow.getContentManager().addContent(content);
        designerUI.initialiseIkasanModel();
    }

}
