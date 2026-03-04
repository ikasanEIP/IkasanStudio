package org.ikasan.studio.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
/**
 * Create all onscreen components and register inter-thread communication components with uiContext
 */
@SuppressWarnings("rawtypes")
public class DesignerUI {
    public static final Logger LOG = Logger.getInstance("DesignerUI");
    private final Project project;
    @SuppressWarnings("rawtypes")
    private final JBPanel mainJPanel = new JBPanel();
    JBTabbedPane paletteAndProperties = new JBTabbedPane();

    /**
     * Create the main Designer window
     * @param toolWindow is the Intellij frame in which this resides
     * @param project is the current Intellij project
     */
    public DesignerUI(ToolWindow toolWindow, Project project) {
        this.project = project; // assign correctly
        project.getService(UiContext.class).setViewHandlerFactory(new ViewHandlerCache(this.project));
        paletteAndProperties.setBorder(JBUI.Borders.empty());
        // Use JBTabbedPane which respects IDE theme by default; avoid forcing a custom UI delegate.

        paletteAndProperties.setBorder(JBUI.Borders.empty());
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setRightTabbedPane(paletteAndProperties);
        if (uiContext.getPipsiIkasanModel() == null) {
            uiContext.setPipsiIkasanModel(new PIPSIIkasanModel(this.project));
        }

        ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(project, false);
        uiContext.setPropertiesPanel(componentPropertiesPanel);
        ComponentPropertiesTabPanel componentPropertiesTabPanel = new ComponentPropertiesTabPanel(componentPropertiesPanel);
        uiContext.setPropertiesTabPanel(componentPropertiesTabPanel);

        paletteAndProperties.addTab(null, componentPropertiesTabPanel);
        paletteAndProperties.setTabComponentAt(0, createSpacedLabel(UiContext.PROPERTIES_TAB_TITLE, 13, 0, 13, 0));
        paletteAndProperties.setBorder(JBUI.Borders.empty());
        JSplitPane propertiesAndCanvasSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new CanvasPanel(this.project),
                paletteAndProperties
        );

        propertiesAndCanvasSplitPane.setBorder(JBUI.Borders.empty());
        propertiesAndCanvasSplitPane.setDividerSize(2);
        propertiesAndCanvasSplitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(StudioUIUtils.getLineColor());
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        // don't call super.paint() which would put in the bevel.
                    }
                };
            }
        });
        mainJPanel.setLayout(new BorderLayout());
        mainJPanel.add(propertiesAndCanvasSplitPane, BorderLayout.CENTER);
        uiContext.setDesignerUI(this);
    }

    protected JLabel createSpacedLabel(String title, int top, int left, int bottom, int right) {
        JLabel label = new JLabel(title);
        label.setBorder(JBUI.Borders.empty(top, left, bottom, right)); // top, left, bottom, right
        return label;
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
            UiContext uiContext = project.getService(UiContext.class);
            DesignerCanvas canvasPanel = uiContext.getDesignerCanvas();
            if (canvasPanel != null) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    StudioPsiUtils.synchGenerateModelInstanceFromJSON(project);
                    // PaletteTabPanel construction and UI changes must run on the EDT. Move to invokeLater.
                    ApplicationManager.getApplication().invokeLater(() -> {
                        PaletteTabPanel paletteTabPanel = new PaletteTabPanel(project);
                        uiContext.setPalettePanel(paletteTabPanel);
                        paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
                        uiContext.setRightTabbedPaneFocus(UiContext.PALETTE_TAB_INDEX);
                    });
                });
            }
        });
    }
}
