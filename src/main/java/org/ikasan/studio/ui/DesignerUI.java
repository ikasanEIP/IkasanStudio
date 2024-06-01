package org.ikasan.studio.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
/**
 * Create all onscreen components and register inter-thread communication components with UiContext
 */
public class DesignerUI {
    public static final Logger LOG = Logger.getInstance("DesignerUI");
    private final String projectKey;
    private final Project project;
    private final JBPanel mainJPanel = new JBPanel();
    JBTabbedPane paletteAndProperties = new JBTabbedPane();

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

        paletteAndProperties.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return 47; // Make palette top panel same height as designer top panel
            }
        });
        UiContext.setRightTabbedPane(projectKey, paletteAndProperties);
        if (UiContext.getPipsiIkasanModel(projectKey) == null) {
            UiContext.setPipsiIkasanModel(projectKey, new PIPSIIkasanModel(projectKey));
        }

        ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, false);
        UiContext.setPropertiesPanel(projectKey, componentPropertiesPanel);
        ComponentPropertiesTabPanel componentPropertiesTabPanel = new ComponentPropertiesTabPanel(componentPropertiesPanel);
        UiContext.setPropertiesTabPanel(projectKey, componentPropertiesTabPanel);
        paletteAndProperties.addTab(UiContext.PROPERTIES_TAB_TITLE, componentPropertiesTabPanel);

        JSplitPane propertiesAndCanvasSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new CanvasPanel(projectKey),
                paletteAndProperties
        );
        propertiesAndCanvasSplitPane.setDividerSize(3);


        mainJPanel.setLayout(new BorderLayout());
        mainJPanel.add(propertiesAndCanvasSplitPane, BorderLayout.CENTER);
        UiContext.setDesignerUI(projectKey, this);
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
                StudioUIUtils.resetModelFromDisk(projectKey);
                PaletteTabPanel paletteTabPanel = new PaletteTabPanel(projectKey);
                UiContext.setPalettePanel(projectKey, paletteTabPanel);
                paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
                UiContext.setRightTabbedPaneFocus(projectKey, UiContext.PALETTE_TAB_INDEX);
            }
        });
    }
}
