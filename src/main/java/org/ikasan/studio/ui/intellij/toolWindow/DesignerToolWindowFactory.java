package org.ikasan.studio.ui.intellij.toolWindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.ikasan.studio.ui.DesignerUI;
import org.ikasan.studio.ui.intellij.IkasanStudioSettings;
import org.jetbrains.annotations.NotNull;

public class DesignerToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance("#DesignerToolWindowFactory");
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (project.isDisposed()) {
            LOG.info("STUDIO: INFO: During call to createToolWindowContent, the project has already been disposed, project will not be saved.");
            return; // Skip initialization for disposed projects
        }

        // Apply the user's preferred mode (Settings → Tools → Ikasan Studio).
        // Defaults to DOCKED so drag-and-drop between the Palette and the Canvas works reliably.
        // SLIDING mode overlays the editor but IntelliJ's global mouse tracking collapses the
        // panel mid-drag (an IntelliJ Platform limitation with no supported workaround).
        // In either mode we maximise on open so the canvas has maximum available width.
        toolWindow.setType(
                IkasanStudioSettings.isDockedModeEnabled() ? ToolWindowType.DOCKED : ToolWindowType.SLIDING,
                null);

        DesignerUI designerUI = new DesignerUI(project);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(designerUI.getContent(), "", false);
        // Get the Module metadata established before anything else.
        toolWindow.getContentManager().addContent(content);
        designerUI.initialiseIkasanModel();

        maximiseOnFirstShow(project, toolWindow);
    }

    /**
     * Maximises the tool window on each IDE session's first content creation.
     * Uses ToolWindowManagerEx.setMaximized() which works for both SLIDING and DOCKED modes,
     * unlike ToolWindowEx.stretchWidth() which only moves the JSplitter for DOCKED windows.
     * Double-nested invokeLater ensures the tool window frame is fully realised before the
     * maximise call; the inner lambda runs after the outer one completes its layout pass.
     */
    private void maximiseOnFirstShow(Project project, ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() ->
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) return;
                try {
                    ToolWindowManager mgr = ToolWindowManager.getInstance(project);
                    if (mgr instanceof ToolWindowManagerEx) {
                        mgr.setMaximized(toolWindow, true);
                    }
                } catch (Exception e) {
                    LOG.warn("STUDIO: Could not maximise Ikasan Studio tool window", e);
                }
            })
        );
    }

}
