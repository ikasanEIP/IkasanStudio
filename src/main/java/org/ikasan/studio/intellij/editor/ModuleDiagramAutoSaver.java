package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.io.File;

/**
 * Optional, best-effort "save a ModuleDiagram-&lt;name&gt;.png at the project root whenever the project
 * closes" - see {@link IkasanStudioSettings#isAutoSaveModuleDiagramOnCloseEnabled()}. Lets a developer
 * browsing many project folders on disk (or a file manager's thumbnail view) recognise a module by its
 * diagram without opening each one in Studio first.
 * <p>
 * Deliberately silent either way (no notification/dialog) and best-effort: a failure here must never block or
 * interrupt the project actually closing, and must never surface as an uncaught plugin exception (see
 * CLAUDE.md's "never let exceptions bubble up to IntelliJ").
 */
public final class ModuleDiagramAutoSaver {
    private static final Logger LOG = Logger.getInstance("#ModuleDiagramAutoSaver");

    private ModuleDiagramAutoSaver() {}

    /** Called from {@link IkasanStudioEditorService}'s {@code ProjectManagerListener#projectClosing}. */
    public static void saveOnProjectClose(Project project) {
        if (!IkasanStudioSettings.isAutoSaveModuleDiagramOnCloseEnabled() || project.isDisposed()) {
            return;
        }
        Runnable task = () -> {
            try {
                saveNow(project);
            } catch (Exception e) {
                LOG.warn("STUDIO: Could not auto-save module diagram on project close", e);
            }
        };
        // projectClosing is expected to already run on the EDT, but the paint work saveDiagramSilently does
        // (Swing setSize/paint) requires it - fall back to a blocking dispatch rather than skip the save if
        // that assumption is ever wrong, since this must complete before the project actually finishes closing.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(task);
        }
    }

    private static void saveNow(Project project) throws java.io.IOException {
        if (project.isDisposed()) {
            return;
        }
        UiContext uiContext = project.getService(UiContext.class);
        Module module = uiContext.getIkasanModule();
        String basePath = project.getBasePath();
        if (module == null || module.getIdentity() == null || basePath == null) {
            return;
        }

        DesignerCanvas canvas = uiContext.getDesignerCanvas();
        boolean ownCanvas = canvas == null;
        try {
            if (ownCanvas) {
                // The Studio editor tab is already closed - UiContext#clearDesignerUI cleared the canvas and
                // its view-handler cache together, while preserving the module model itself. Build the same
                // throwaway rendering support DesignerCanvas' own tests use, just for this one export.
                uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
                canvas = new DesignerCanvas(project);
            }
            File file = new File(basePath, DesignerCanvas.moduleDiagramFileName(module));
            canvas.saveDiagramSilently(file, "png", false);
        } finally {
            if (ownCanvas) {
                uiContext.setViewHandlerFactory(null);
                // The paint pass saveDiagramSilently just ran can start DesignerCanvas' own flow-error-flash
                // Timer; since nothing else holds or will ever use this throwaway canvas again, it must be
                // disposed here or that Timer leaks for the rest of the IDE session.
                if (canvas != null) {
                    canvas.disposeCanvas();
                }
            }
        }
    }
}
