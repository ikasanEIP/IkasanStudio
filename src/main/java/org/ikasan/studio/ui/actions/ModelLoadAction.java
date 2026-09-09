package org.ikasan.studio.ui.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * "Reload from Disk" - re-reads the persisted model.json into the in-memory module, then re-renders the canvas.
 * Model.json import deliberately does not use this (see ModelImporter): the PSI document can still hold the
 * pre-import text immediately after the import's direct disk write, so the re-read would race the write.
 */
public class ModelLoadAction implements ActionListener {
    private final Project project;

    public ModelLoadAction(Project project) {
        this.project = project;
    }

    /**
     * Re-reads model.json from disk on a background thread (PSI/VFS reads must stay off the EDT) and repaints the
     * canvas on the EDT once the in-memory module has been replaced.
     */
    public static void reloadModelFromDisk(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            StudioProjectFiles.synchGenerateModelInstanceFromJSON(project);
            // All UI operations must run on EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                UiContext uiContext = project.getService(UiContext.class);
                if (uiContext.getIkasanModule() == null) {
                    return;
                }
                if (uiContext.getCanvasPanel() != null) {
                    uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
                }
                if (uiContext.getPalettePanel() != null) {
                    uiContext.getPalettePanel().resetPallette();
                }
                DesignerCanvas canvas = uiContext.getDesignerCanvas();
                if (canvas != null && !canvas.isDisposed()) {
                    canvas.disableModuleInitialiseProcess();
                    canvas.setInitialiseAllDimensions(true);
                    canvas.revalidate();
                    canvas.repaint();
                }
                if (uiContext.getPalettePanel() != null) {
                    uiContext.getPalettePanel().repaint();
                }
            });
        });
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.LoadJsonModelFromFile"));
        reloadModelFromDisk(project);
    }
}
