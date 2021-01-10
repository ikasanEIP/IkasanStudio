package org.ikasan.studio.ui;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.ui.component.CanvasPanel;
import org.ikasan.studio.ui.component.DesignerCanvas;
import org.ikasan.studio.ui.component.PalettePanel;
import org.ikasan.studio.ui.component.PropertiesPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Create all onscreen components and register inter-thread communication components with Context
 */
public class DesignerUI {
    private String projectKey;
    private Project project;
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    private JPanel mainJPanel = new JPanel();
//    private IkasanFlowUIComponentSelection ikasanFlowUIComponentSelection = new IkasanFlowUIComponentSelection();
    /**
     * Create the main Designer window
     * @param toolWindow is the Intellij frame in which this resides
     * @param project is the Java project
     */
    public DesignerUI(ToolWindow toolWindow, Project project) {
        this.project = project;
        this.projectKey = project.getName();
        Context.setProject(projectKey, project);
//        String projectKey = project.getName();
//        IkasanModule ikasanModule = Context.getIkasanModule(projectKey);
        if (Context.getIkasanModule(projectKey) == null) {
            Context.setIkasanModule(projectKey, new IkasanModule());
        }
        JSplitPane propertiesAndCanvasSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new PropertiesPanel(projectKey),
                new CanvasPanel(projectKey, toolWindow));
        propertiesAndCanvasSplitPane.setDividerSize(3);
        propertiesAndCanvasSplitPane.setDividerLocation(0.4);
        Context.setPropertiesAndCanvasPane(projectKey, propertiesAndCanvasSplitPane);

        mainJPanel.setLayout(new BorderLayout());
        mainJPanel.add(propertiesAndCanvasSplitPane, BorderLayout.CENTER);
        mainJPanel.add(new PalettePanel(projectKey), BorderLayout.EAST);
    }

    public JPanel getContent() {
        return mainJPanel;
    }

    /**
     * This will populate the canvas as soon as the indexing service has completed
     * Note, it may result in an IndexNotReadyException but seems tro retry successfully.
     */
    public void initialiseIkasanModel() {
        DumbService dumbService = DumbService.getInstance(project);
        dumbService.runWhenSmart(new Runnable() {
            @Override
            public void run() {
                DesignerCanvas canvasPanel = Context.getDesignerCanvas(projectKey);
                if (canvasPanel != null) {
                    StudioPsiUtils.resetIkasanModuleFromSourceCode(projectKey, false);
//                    canvasPanel.setIkasanModule(StudioPsiUtils.updateIkasanModuleFromSourceCode(projectKey, false));
                }
            }
        });
    }
}
