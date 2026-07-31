package org.ikasan.studio.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.StudioInitialisationPanel;
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
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Create all onscreen components and register inter-thread communication components with uiContext
 */
public class DesignerUI {
    public static final Logger LOG = Logger.getInstance("DesignerUI");
    private final Project project;
    private static final String INITIALISING_CARD = "initialising";
    private static final String DESIGNER_CARD = "designer";
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final StudioInitialisationPanel initialisationPanel;
    private final AtomicBoolean initialisationInProgress = new AtomicBoolean();
    JBTabbedPane paletteAndProperties = new JBTabbedPane();
    JSplitPane propertiesAndCanvasSplitPane;

    /**
     * Create the main Designer window, this contains ALL the Ikasan Studio elements except source code.
     * @param project is the current Intellij project
     */
    public DesignerUI(Project project) {
        this.project = project;
        this.initialisationPanel = new StudioInitialisationPanel(this::initialiseIkasanModel);
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
        contentPanel.add(initialisationPanel, INITIALISING_CARD);
        contentPanel.add(propertiesAndCanvasSplitPane, DESIGNER_CARD);
        contentLayout.show(contentPanel, INITIALISING_CARD);
        uiContext.setDesignerUI(this);
    }

    protected JLabel createPropertiesLabel() {
        JLabel label = new JLabel(UiContext.PROPERTIES_TAB_TITLE);
        label.setBorder(JBUI.Borders.empty(13, 0)); // top, left, bottom, right
        return label;
    }

    public JComponent getContent() {
        return contentPanel;
    }

    /**
     * This will populate the canvas as soon as the indexing service has completed
     * Note, it may result in an IndexNotReadyException but seems to retry successfully.
     */
    public void initialiseIkasanModel() {
        if (!initialisationInProgress.compareAndSet(false, true)) {
            return;
        }
        showInitialisationState(initialisationPanel::showWaitingForIndexes);
        DumbService dumbService = DumbService.getInstance(project);
        dumbService.runWhenSmart(() -> {
            if (project.isDisposed()) {
                initialisationInProgress.set(false);
                return;
            }
            UiContext uiContext = project.getService(UiContext.class);
            DesignerCanvas canvasPanel = uiContext.getDesignerCanvas();
            if (canvasPanel != null) {
                showInitialisationState(initialisationPanel::showReadingProject);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        StudioPsiUtils.synchGenerateModelInstanceFromJSON(project);
                        uiContext.getPipsiIkasanModel().initialisePsiFileHandles();
                        showInitialisationState(initialisationPanel::showLoadingComponents);
                        // PaletteTabPanel construction and UI changes must run on the EDT.
                        ApplicationManager.getApplication().invokeLater(() -> completeInitialisation(uiContext));
                    } catch (ProcessCanceledException e) {
                        initialisationInProgress.set(false);
                        throw e;
                    } catch (Exception e) {
                        LOG.warn("STUDIO: Could not initialise Ikasan Studio", e);
                        showInitialisationFailure("Check that the Maven project has imported successfully, then try again.");
                    }
                });
            } else {
                showInitialisationFailure("The Ikasan designer could not be created. Try reopening the tool window.");
            }
        });
    }

    private void completeInitialisation(UiContext uiContext) {
        if (project.isDisposed()) {
            initialisationInProgress.set(false);
            return;
        }
        try {
            PaletteTabPanel paletteTabPanel = uiContext.getPalettePanel();
            if (paletteTabPanel == null) {
                paletteTabPanel = new PaletteTabPanel(project);
                uiContext.setPalettePanel(paletteTabPanel);
                paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
            }
            uiContext.setRightTabbedPaneFocus(UiContext.PALETTE_TAB_INDEX);
            if (uiContext.getIkasanModule() == null) {
                throw new IllegalStateException("No Ikasan module model was created");
            }
            uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
            contentLayout.show(contentPanel, DESIGNER_CARD);
            initialisationInProgress.set(false);

            PaletteTabPanel finalPaletteTabPanel = paletteTabPanel;
            // Defer divider positioning until Swing has completed the new tab's layout pass.
            ApplicationManager.getApplication().invokeLater(() -> {
                int paletteWidth = finalPaletteTabPanel.getPaletteScrollPanePreferredWidth();
                int splitWidth = propertiesAndCanvasSplitPane.getWidth();
                if (paletteWidth > 0 && splitWidth > paletteWidth) {
                    propertiesAndCanvasSplitPane.setDividerLocation(
                            splitWidth - paletteWidth - propertiesAndCanvasSplitPane.getDividerSize());
                }
            });
        } catch (ProcessCanceledException e) {
            initialisationInProgress.set(false);
            throw e;
        } catch (Exception e) {
            LOG.warn("STUDIO: Could not complete Ikasan Studio initialisation", e);
            showInitialisationFailure("The project model or component library could not be loaded. Review the IDE log for details, then try again.");
        }
    }

    private void showInitialisationFailure(String detail) {
        initialisationInProgress.set(false);
        showInitialisationState(() -> initialisationPanel.showFailure(detail));
    }

    private void showInitialisationState(Runnable update) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                contentLayout.show(contentPanel, INITIALISING_CARD);
                update.run();
            }
        });
    }
}
