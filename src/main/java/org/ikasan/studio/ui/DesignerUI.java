package org.ikasan.studio.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
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
    JTabbedPane paletteAndProperties = new JTabbedPane();

    /**
     * Create the main Designer window
     * @param toolWindow is the Intellij frame in which this resides
     * @param project is the Java project
     */
    public DesignerUI(ToolWindow toolWindow, Project project) {
        this.project = project;
        this.projectKey = project.getName();
        UiContext.setProject(projectKey, project);
        UiContext.setViewHandlerFactory(projectKey, new ViewHandlerCache(projectKey));
        paletteAndProperties.setBorder(new EmptyBorder(0,0,0,0));
        paletteAndProperties.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0); // Adjust padding around tabs if necessary
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                // Do nothing or draw a flat border if needed
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Optionally, remove or customize the content border
            }
        });


        paletteAndProperties.setBorder(new EmptyBorder(0, 0, 0, 0));
        UiContext.setRightTabbedPane(projectKey, paletteAndProperties);
        if (UiContext.getPipsiIkasanModel(projectKey) == null) {
            UiContext.setPipsiIkasanModel(projectKey, new PIPSIIkasanModel(projectKey));
        }

        ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, false);
        UiContext.setPropertiesPanel(projectKey, componentPropertiesPanel);
        ComponentPropertiesTabPanel componentPropertiesTabPanel = new ComponentPropertiesTabPanel(componentPropertiesPanel);
        UiContext.setPropertiesTabPanel(projectKey, componentPropertiesTabPanel);

        paletteAndProperties.addTab(null, componentPropertiesTabPanel);
        paletteAndProperties.setTabComponentAt(0, createSpacedLabel(UiContext.PROPERTIES_TAB_TITLE, 13, 0, 13, 0));
        paletteAndProperties.setBorder(new EmptyBorder(0, 0, 0, 0));
        JSplitPane propertiesAndCanvasSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new CanvasPanel(projectKey),
                paletteAndProperties
        );

        propertiesAndCanvasSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
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
        UiContext.setDesignerUI(projectKey, this);
    }

    protected JLabel createSpacedLabel(String title, int top, int left, int bottom, int right) {
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right)); // top, left, bottom, right
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
            DesignerCanvas canvasPanel = UiContext.getDesignerCanvas(projectKey);
            if (canvasPanel != null) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    StudioPsiUtils.synchGenerateModelInstanceFromJSON(projectKey);
                    PaletteTabPanel paletteTabPanel = new PaletteTabPanel(projectKey);
                    UiContext.setPalettePanel(projectKey, paletteTabPanel);
                    paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
                    UiContext.setRightTabbedPaneFocus(projectKey, UiContext.PALETTE_TAB_INDEX);
                });
            }
        });
    }
}
