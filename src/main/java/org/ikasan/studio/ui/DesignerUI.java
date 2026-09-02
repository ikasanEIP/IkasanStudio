package org.ikasan.studio.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.StudioInitialisationPanel;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
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
    // Persists the palette/properties panel's width (not the raw divider location, which is meaningless
    // across sessions/window sizes on its own) so the user only ever needs to drag it once.
    private static final String RIGHT_PANEL_WIDTH_PROPERTY = "ikasan.studio.designer.rightPanelWidth";
    private static final int NO_PERSISTED_WIDTH = -1;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final StudioInitialisationPanel initialisationPanel;
    private final StudioProjectInitialisationService initialisationService;
    JBTabbedPane paletteAndProperties = new JBTabbedPane();
    JSplitPane propertiesAndCanvasSplitPane;
    // Guards the persistence listener below against our own programmatic setDividerLocation() calls -
    // without this, restoring a persisted width while the split pane hasn't yet been laid out to its
    // real size (getWidth() still small/stale during early startup) computes a wrong divider location,
    // which the listener would then immediately re-persist, silently corrupting the user's saved width
    // on every single restart.
    private boolean restoringDividerLocation = false;

    /**
     * Create the main Designer window, this contains ALL the Ikasan Studio elements except source code.
     * @param project is the current Intellij project
     */
    public DesignerUI(Project project) {
        this.project = project;
        this.initialisationService = project.getService(StudioProjectInitialisationService.class);
        this.initialisationPanel = new StudioInitialisationPanel(initialisationService::start);
        project.getMessageBus().connect(this).<StudioProjectInitialisationService.Listener>subscribe(
                StudioProjectInitialisationService.Listener.TOPIC,
                this::initialisationStateChanged);
        project.getService(UiContext.class).setViewHandlerFactory(new ViewHandlerCache(this.project));
        paletteAndProperties.setBorder(JBUI.Borders.empty());

        paletteAndProperties.setBorder(JBUI.Borders.empty());
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setRightTabbedPane(paletteAndProperties);
        if (uiContext.getPipsiIkasanModel() == null) {
            uiContext.setPipsiIkasanModel(new GeneratedProjectSynchronizer(this.project));
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
        // Remember whatever width the user leaves the panel at (whether from a manual drag or from the
        // programmatic sizing below), so it doesn't need re-dragging on every project open.
        propertiesAndCanvasSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (restoringDividerLocation) {
                return;
            }
            int rightPanelWidth = getRightPanelWidth();
            if (rightPanelWidth > 0) {
                PropertiesComponent.getInstance(project).setValue(
                        RIGHT_PANEL_WIDTH_PROPERTY, rightPanelWidth, NO_PERSISTED_WIDTH);
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

    /**
     * The palette/properties panel's current on-screen width, derived from the split pane's own state
     * (divider location is only meaningful relative to the split's total width, which varies with the
     * window/tool-window size, so the width - not the raw location - is what's persisted and restored).
     */
    private int getRightPanelWidth() {
        return propertiesAndCanvasSplitPane.getWidth()
                - propertiesAndCanvasSplitPane.getDividerLocation()
                - propertiesAndCanvasSplitPane.getDividerSize();
    }

    /**
     * Re-sizes the palette/properties panel to fit newly-available content. The first sizing pass (in
     * completeInitialisation) runs before a fresh project's module is configured, when the Palette is
     * still empty and there's nothing meaningful to size against - so once the Palette is repopulated
     * with its real component list (see PaletteTabPanel#resetPallette), it asks for this re-sizing so
     * the panel grows to fit that content instead of staying at its empty-state width.
     */
    public void resizeRightPanelToContent() {
        if (project.isDisposed()) {
            return;
        }
        UiContext uiContext = project.getService(UiContext.class);
        PaletteTabPanel paletteTabPanel = uiContext.getPalettePanel();
        if (paletteTabPanel != null) {
            ApplicationManager.getApplication().invokeLater(
                    () -> applyRightPanelWidth(uiContext, paletteTabPanel, 0));
        }
    }

    private static final int MAX_APPLY_WIDTH_RETRIES = 10;

    /**
     * Sizes the palette/properties panel to its persisted width (or, on a project's first ever launch, the
     * larger of the Palette/Properties tabs' natural content widths). Retries itself a bounded number of
     * times if the split pane hasn't been laid out to a real size yet - early in startup getWidth() can
     * still be reporting a small/stale value, and computing/applying a divider location against that would
     * either be rejected (Swing clamps an out-of-range location) or, worse, get immediately re-persisted by
     * the divider-location listener, corrupting the user's saved width. Only ever runs on the EDT.
     */
    private void applyRightPanelWidth(UiContext uiContext, PaletteTabPanel finalPaletteTabPanel, int attempt) {
        int persistedWidth = PropertiesComponent.getInstance(project)
                .getInt(RIGHT_PANEL_WIDTH_PROPERTY, NO_PERSISTED_WIDTH);
        // Only the Palette tab is actually visible right now (it's the one just force-selected in
        // completeInitialisation), but Properties' own preferred width is consulted too - otherwise a
        // project's very first launch sizes the panel for the narrower Palette, and switching to Properties
        // immediately feels cramped.
        int rightPanelWidth = persistedWidth > 0
                ? persistedWidth
                : Math.max(
                        finalPaletteTabPanel.getPaletteScrollPanePreferredWidth(),
                        uiContext.getPropertiesTabPanel().getPropertiesPreferredWidth());
        int splitWidth = propertiesAndCanvasSplitPane.getWidth();
        if (rightPanelWidth <= 0) {
            return;
        }
        if (splitWidth <= rightPanelWidth) {
            // Not laid out to a usable size yet - give Swing another turn of the event loop rather than
            // applying (and potentially persisting) a divider location computed from this unreliable width.
            if (attempt < MAX_APPLY_WIDTH_RETRIES) {
                ApplicationManager.getApplication().invokeLater(
                        () -> applyRightPanelWidth(uiContext, finalPaletteTabPanel, attempt + 1));
            }
            return;
        }
        restoringDividerLocation = true;
        try {
            propertiesAndCanvasSplitPane.setDividerLocation(
                    splitWidth - rightPanelWidth - propertiesAndCanvasSplitPane.getDividerSize());
        } finally {
            restoringDividerLocation = false;
        }
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
            // Restore whatever was selected before this DesignerUI was (re)created - the editor can be
            // closed and recreated (e.g. by the platform on tab/focus churn) without the user having
            // explicitly changed their selection, and that shouldn't bounce Properties back to the Module.
            // Only default to the Module when nothing has ever been selected (fresh launch), so Properties
            // has useful default content instead of sitting blank until the user first clicks something.
            IkasanObject previouslySelected = uiContext.getSelectedComponent();
            IkasanObject componentToShow = previouslySelected != null ? previouslySelected : uiContext.getIkasanModule();
            // A brand-new project's module has no version chosen yet (the placeholder "dumb" module has
            // no component metadata) - setup for that case is driven by the canvas's own start button/version
            // picker, so there is nothing valid to show in Properties until the module is actually initialised.
            if (componentToShow instanceof Module module && !module.isInitialised()) {
                componentToShow = null;
            }
            if (componentToShow != null) {
                uiContext.getPropertiesTabPanel().updateTargetComponent(componentToShow);
            }
            // Defer divider positioning until Swing has completed the new tab's layout pass.
            ApplicationManager.getApplication().invokeLater(
                    () -> applyRightPanelWidth(uiContext, finalPaletteTabPanel, 0));
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
        if (!project.isDisposed()) {
            project.getService(UiContext.class).clearDesignerUI(this);
        }
    }
}
