package org.ikasan.studio.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.StudioInitialisationPanel;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.intellij.StudioProjectInitialisationService;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
/**
 * Create all onscreen components and register inter-thread communication components with uiContext
 */
public class DesignerUI implements Disposable {
    private final Project project;
    private static final String INITIALISING_CARD = "initialising";
    private static final String DESIGNER_CARD = "designer";
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final StudioInitialisationPanel initialisationPanel;
    private final StudioProjectInitialisationService initialisationService;
    JBTabbedPane paletteAndProperties = new JBTabbedPane();
    JSplitPane propertiesAndCanvasSplitPane;

    /**
     * Create the main Designer window, this contains ALL the Ikasan Studio elements except source code.
     * @param project is the current Intellij project
     */
    public DesignerUI(Project project) {
        this.project = project;
        this.initialisationService = project.getService(StudioProjectInitialisationService.class);
        this.initialisationPanel = new StudioInitialisationPanel(initialisationService::retry);
        project.getMessageBus().connect(this).<StudioProjectInitialisationService.Listener>subscribe(
                StudioProjectInitialisationService.Listener.TOPIC,
                this::initialisationStateChanged);
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
        Disposer.register(this, canvasPanel);
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
        if (initialisationService.getState() == StudioProjectInitialisationService.State.READY) {
            completeInitialisation(uiContext);
        } else {
            initialisationStateChanged(initialisationService.getState(), initialisationService.getDetail());
            initialisationService.start();
        }
    }

    protected JLabel createPropertiesLabel() {
        JLabel label = new JLabel(UiContext.PROPERTIES_TAB_TITLE);
        label.setBorder(JBUI.Borders.empty(13, 0)); // top, left, bottom, right
        return label;
    }

    public JComponent getContent() {
        return contentPanel;
    }

    private void completeInitialisation(UiContext uiContext) {
        if (project.isDisposed()) {
            return;
        }
        try {
            PaletteTabPanel paletteTabPanel = uiContext.getPalettePanel();
            if (paletteTabPanel == null) {
                paletteTabPanel = new PaletteTabPanel(project);
                uiContext.setPalettePanel(paletteTabPanel);
            }
            if (paletteAndProperties.indexOfComponent(paletteTabPanel) < 0) {
                paletteAndProperties.addTab(UiContext.PALETTE_TAB_TITLE, paletteTabPanel);
            }
            uiContext.setRightTabbedPaneFocus(UiContext.PALETTE_TAB_INDEX);
            if (uiContext.getIkasanModule() == null) {
                throw new IllegalStateException("No Ikasan module model was created");
            }
            uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
            initialisationService.markReady();

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
            throw e;
        } catch (Exception e) {
            initialisationService.fail(
                    "The Ikasan component library could not be loaded. Review the IDE log for details, then try again.", e);
        }
    }

    private void initialisationStateChanged(StudioProjectInitialisationService.State state, String detail) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            contentLayout.show(contentPanel, state == StudioProjectInitialisationService.State.READY
                    ? DESIGNER_CARD : INITIALISING_CARD);
            switch (state) {
                case WAITING_FOR_PROJECT_IMPORT -> initialisationPanel.showWaitingForProjectImport();
                case WAITING_FOR_INDEXES -> initialisationPanel.showWaitingForIndexes();
                case READING_PROJECT -> initialisationPanel.showReadingProject();
                case LOADING_COMPONENTS -> {
                    initialisationPanel.showLoadingComponents();
                    completeInitialisation(project.getService(UiContext.class));
                }
                case FAILED -> initialisationPanel.showFailure(detail);
                default -> { }
            }
        });
    }

    @Override
    public void dispose() {
        // The message-bus connection is disposed with this UI instance.
    }
}
