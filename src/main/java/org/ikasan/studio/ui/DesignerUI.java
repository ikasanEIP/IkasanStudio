package org.ikasan.studio.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PalettePanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static org.ikasan.studio.ui.UiContext.JSON_MODEL_FILE_WITH_EXTENSION;
/**
 * Create all onscreen components and register inter-thread communication components with UiContext
 */
public class DesignerUI {
    public static final Logger LOG = Logger.getInstance("DesignerUI");
    private final String projectKey;
    private final Project project;
    private final JPanel mainJPanel = new JPanel();

    /**
     * Create the main Designer window
     * @param toolWindow is the Intellij frame in which this resides
     * @param project is the Java project
     */
    public DesignerUI(ToolWindow toolWindow, Project project) {
        this.project = project;
        this.projectKey = project.getName();
        UiContext.setProject(projectKey, project);
        UiContext.setViewHandlerFactory(projectKey, new ViewHandlerFactoryIntellij(projectKey));
        if (UiContext.getPipsiIkasanModel(projectKey) == null) {
            UiContext.setPipsiIkasanModel(projectKey, new PIPSIIkasanModel(projectKey));
        }

        ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, false);
        UiContext.setPropertiesPanel(projectKey, componentPropertiesPanel);

        JSplitPane propertiesAndCanvasSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                componentPropertiesPanel,
                new CanvasPanel(projectKey));
        propertiesAndCanvasSplitPane.setDividerSize(3);
        propertiesAndCanvasSplitPane.setDividerLocation(0.4);
        UiContext.setPropertiesAndCanvasPane(projectKey, propertiesAndCanvasSplitPane);

        mainJPanel.setLayout(new BorderLayout());
        mainJPanel.add(propertiesAndCanvasSplitPane, BorderLayout.CENTER);
    }

    public JPanel getContent() {
        return mainJPanel;
    }

    /**
     * This will populate the canvas as soon as the indexing service has completed
     * Note, it may result in an IndexNotReadyException but seems to retry successfully.
     */
    public void initialiseIkasanModel() {
        DumbService dumbService = DumbService.getInstance(project);
        dumbService.runWhenSmart(() -> {
            DesignerCanvas canvasPanel = UiContext.getDesignerCanvas(projectKey);
            if (canvasPanel != null) {
                try {
                    StudioPsiUtils.generateModelInstanceFromJSON(projectKey, false);
                } catch (StudioBuildException se) {
                    LOG.warn("SERIOUS ERROR: Reported when reading " + JSON_MODEL_FILE_WITH_EXTENSION + Arrays.asList(se.getStackTrace()));
                    StudioUIUtils.displayIdeaInfoMessage(projectKey, "Error: Please check " + JSON_MODEL_FILE_WITH_EXTENSION + " for errors");
                    // The dumb module should contain just enough to prevent the plugin from crashing
                    UiContext.setIkasanModule(projectKey, Module.getDumbModule());
                }
                PalettePanel palettePanel = new PalettePanel(projectKey);
                UiContext.setPalettePanel(projectKey, palettePanel);
                mainJPanel.add(palettePanel, BorderLayout.EAST);
            }
        });
    }
}
