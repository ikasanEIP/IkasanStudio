package org.ikasan.studio.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
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
public class DesignerUI {
    public static final Logger LOG = Logger.getInstance("DesignerUI");
    private final Project project;
    JBTabbedPane paletteAndProperties = new JBTabbedPane();
    JSplitPane propertiesAndCanvasSplitPane;

    /**
     * Create the main Designer window, this contains ALL the Ikasan Studio elements except source code.
     * @param project is the current Intellij project
     */
    public DesignerUI(Project project) {
        this.project = project;
        project.getService(UiContext.class).setViewHandlerFactory(new ViewHandlerCache(this.project));
        paletteAndProperties.setBorder(JBUI.Borders.empty());

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

        paletteAndProperties.add(componentPropertiesTabPanel);
        paletteAndProperties.setTabComponentAt(0, createPropertiesLabel());
        paletteAndProperties.setBorder(JBUI.Borders.empty());

        CanvasPanel canvasPanel = new CanvasPanel(this.project);
        uiContext.setCanvasPanel(canvasPanel);
        propertiesAndCanvasSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                canvasPanel,
                paletteAndProperties
        );

        propertiesAndCanvasSplitPane.setBorder(JBUI.Borders.empty());
        propertiesAndCanvasSplitPane.setDividerSize(2);
        // Canvas (left) absorbs all extra space when the IDE window is resized;
        // the palette/properties panel (right) stays at its preferred width.
        propertiesAndCanvasSplitPane.setResizeWeight(1.0);
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
        uiContext.setDesignerUI(this);
    }

    protected JLabel createPropertiesLabel() {
        JLabel label = new JLabel(UiContext.PROPERTIES_TAB_TITLE);
        label.setBorder(JBUI.Borders.empty(13, 0)); // top, left, bottom, right
        return label;
    }

    public JComponent getContent() {
        return propertiesAndCanvasSplitPane;
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
                    uiContext.getPipsiIkasanModel().initialisePsiFileHandles();
                    // PaletteTabPanel construction and UI changes must run on the EDT. Move to invokeLater.
                    ApplicationManager.getApplication().invokeLater(() -> {
                        PaletteTabPanel paletteTabPanel = new PaletteTabPanel(project);
                        uiContext.setPalettePanel(paletteTabPanel);
                        paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
                        uiContext.setRightTabbedPaneFocus(UiContext.PALETTE_TAB_INDEX);
                        uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
                        // Defer divider positioning to the next EDT cycle so that Swing has
                        // completed the layout pass for the new tab. At that point BasicListUI
                        // has iterated all cells and font metrics are fully initialised, giving
                        // an accurate preferred width for the palette content.
                        // setDividerLocation() is used instead of resetToPreferredSizes() so the
                        // position is derived from the split pane's actual realised width rather
                        // than preferred-size bookkeeping.
                        ApplicationManager.getApplication().invokeLater(() -> {
                            int paletteWidth = paletteTabPanel.getPaletteScrollPanePreferredWidth();
                            int splitWidth = propertiesAndCanvasSplitPane.getWidth();
                            if (paletteWidth > 0 && splitWidth > paletteWidth) {
                                propertiesAndCanvasSplitPane.setDividerLocation(
                                        splitWidth - paletteWidth - propertiesAndCanvasSplitPane.getDividerSize());
                            }
                        });
                    });
                });
            }
        });
    }
}
