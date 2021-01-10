package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.jetbrains.annotations.NotNull;

public class DesignerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        DesignerWindow myToolWindow = new DesignerWindow(@NotNull Project project, toolWindow);
        DesignerUI designerUI = new DesignerUI(toolWindow, project);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(designerUI.getContent(), "", false);
        // Get the the Module meta data established before anything else.
        toolWindow.getContentManager().addContent(content);
        designerUI.initialiseIkasanModel();
    }

}
