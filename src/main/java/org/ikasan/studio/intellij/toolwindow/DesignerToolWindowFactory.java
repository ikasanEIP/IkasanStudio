package org.ikasan.studio.intellij.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.ikasan.studio.intellij.editor.IkasanStudioEditorService;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

/**
 * Keeps the discoverable tool-window stripe icon, but treats it as a one-click launcher for the
 * editor-owned designer rather than presenting a second Studio panel.
 */
public class DesignerToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "Ikasan Studio";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Content content = ContentFactory.getInstance()
                .createContent(new JBPanel<>(new BorderLayout()), "", false);
        toolWindow.getContentManager().addContent(content);

        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(
                ToolWindowManagerListener.TOPIC,
                new ToolWindowManagerListener() {
                    @Override
                    public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                        if (TOOL_WINDOW_ID.equals(shownToolWindow.getId())) {
                            openEditorAndHideLauncher(project, shownToolWindow);
                        }
                    }
                });

        // Content creation can happen after the first visibility event, so handle that initial
        // activation explicitly as well as subsequent activations handled by the listener.
        ApplicationManager.getApplication().invokeLater(() -> {
            if (toolWindow.isVisible()) {
                openEditorAndHideLauncher(project, toolWindow);
            }
        });
    }

    private static void openEditorAndHideLauncher(Project project, ToolWindow toolWindow) {
        if (project.isDisposed()) {
            return;
        }
        project.getService(IkasanStudioEditorService.class).open();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed() && toolWindow.isVisible()) {
                toolWindow.hide();
            }
        });
    }
}
