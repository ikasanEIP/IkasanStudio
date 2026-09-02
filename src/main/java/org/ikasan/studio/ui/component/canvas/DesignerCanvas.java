package org.ikasan.studio.ui.component.canvas;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.Pair;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.PaintMode;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.NavigateToCodeAction;
import org.ikasan.studio.ui.actions.DeleteComponentUndoableAction;
import org.ikasan.studio.ui.actions.SendTestMessageAction;
import org.ikasan.studio.ui.actions.TriggerScheduledConsumerAction;
import org.ikasan.studio.ui.actions.FlowTransportAction;
import org.ikasan.studio.ui.actions.FlowTransportControlAction;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolverPanel;
import org.ikasan.studio.ui.component.properties.PropertiesPopupDialogue;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.GenerationRequest;
import org.ikasan.studio.ui.model.psi.UserImplementedClassRelocator;
import org.ikasan.studio.ui.intellij.IkasanStudioSettings;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;
import org.ikasan.studio.ui.intellij.FlowErrorMonitorService;
import org.ikasan.studio.ui.intellij.TestMailServerSessionService;
import org.ikasan.studio.ui.theme.ThemeAwareColors;
import org.ikasan.studio.ui.viewmodel.*;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.Timer;
import com.intellij.openapi.ui.ComboBox;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInPascalCase;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME;

/**
 * The main painting / design panel
 */
public class DesignerCanvas extends JPanel {
    private static final Logger LOG = Logger.getInstance("#DesignerCanvas");
    private static final String MODULE_LAUNCHED_PROPERTY = "ikasan.studio.onboarding.moduleLaunched";
    private static final String CONSOLE_OPENED_PROPERTY = "ikasan.studio.onboarding.consoleOpened";
    private boolean initialiseAllDimensions = true;
    private int clickStartMouseX = 0 ;
    private int clickStartMouseY = 0 ;
    private boolean screenChanged = false;
    private static final int COMPONENT_DRAG_THRESHOLD = 5;
    private FlowElement dragCandidate;
    private FlowElement draggedElement;
    private Point dragPoint;
    // Lazily started the first time a flow is flagged in error, stopped again once none are - see
    // updateFlowErrorFlashState(). Kept as a plain Swing Timer, not an Alarm, since it exists purely to drive
    // repaint() on the EDT at a fixed visual cadence - it has no work of its own to do off-EDT.
    private Timer flowErrorFlashTimer;
    private boolean flowErrorFlashOn = false;
    // The OS-level click that brings the whole IDE window back into focus (e.g. after switching to
    // another desktop app) is also delivered to whichever component is underneath it as a genuine
    // MOUSE_PRESSED event. If that happens to land on empty canvas, it would otherwise be treated as a
    // deliberate click to select the Module, silently discarding whatever was selected in Properties.
    // A short suppression window after the application regains OS focus treats that first click as
    // "bring to front" only, not a selection click.
    private static final long SUPPRESS_CLICK_AFTER_APP_REACTIVATION_MS = 500;
    private volatile long applicationReactivatedAtMillis = 0;
    private final Project project;
    private final JButton startButton = new JButton(
        StudioBundle.message("button.ConfigureModule"),
        IconLoader.getIcon("/studio/icons/configure-module.svg", DesignerCanvas.class));
    private final ComboBox<String> metaDataVersionComboBox;
    private final JPanel newModulePanel;

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBackgroundColor() {
        return ThemeAwareColors.getBackgroundColor();
    }

    public DesignerCanvas(Project project) {
        this.project = project;
        setBackground(getThemeAwareBackgroundColor());
        // Plain JPanels aren't focusable by default, so this canvas never became the focus owner - which
        // meant IntelliJ's Edit > Undo/Redo (resolved from the focused component's DataContext via
        // PlatformCoreDataKeys.FILE_EDITOR) could never find this editor and stayed permanently disabled
        // while working here, regardless of what was on the undo stack.
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LOG.trace("STUDIO: Mouse press x "+ e.getX() + " y " + e.getY());
                requestFocusInWindow();
                if (System.currentTimeMillis() - applicationReactivatedAtMillis < SUPPRESS_CLICK_AFTER_APP_REACTIVATION_MS) {
                    applicationReactivatedAtMillis = 0;
                    return;
                }
                mouseClickAction(e, e.getX(),e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                LOG.trace("STUDIO: Mouse release click x "+ e.getX() + " y " + e.getY());
                mouseReleaseAction(e.getX(), e.getY());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                LOG.trace("STUDIO: DesignerCanvas listening to mouse drag x " + e.getX() + " y " + e.getY());
                mouseDragAction(e.getX(),e.getY());
            }
        });


        if (project.getService(UiContext.class).getOptions().isHintsEnabled()) {
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    LOG.trace("STUDIO: DesignerCanvas listening to mouse move x " + e.getX() + " y " + e.getY());
                mouseMoveAction(e.getX(),e.getY());
                }
            });
        }
        setTransferHandler(new CanvasImportTransferHandler( this));

        List<String> installedMetapacks = IkasanComponentLibrary.getMetapackList();
        installedMetapacks.sort(String::compareToIgnoreCase);
        metaDataVersionComboBox = new ComboBox<>(installedMetapacks.toArray(String[]::new));
        metaDataVersionComboBox.getAccessibleContext().setAccessibleName(StudioBundle.message("label.IkasanVersion"));
        startButton.setToolTipText(StudioBundle.message("tooltip.ConfigureThisModuleUsingTheSelectedIkasanVersion"));
        newModulePanel = createNewModulePanel();
        // Create the properties popup panel for a new Module
        startButton.addActionListener(e ->
            {
                UiContext uiContext = project.getService(UiContext.class);
                String metapackVersion = (String) metaDataVersionComboBox.getSelectedItem();
                if (metapackVersion == null) {
                    StudioUIUtils.displayIdeaInfoMessage(this.project,
                            StudioBundle.message("message.ChooseAnIkasanVersionBeforeConfiguringTheModule"));
                } else {
                    if (IkasanComponentLibrary.versionNotContained(metapackVersion)) {
                        try {
                            IkasanComponentLibrary.refreshComponentLibrary(metapackVersion);
                        } catch (StudioBuildException ex) {
                            LOG.warn("STUDIO: Could not load component library " + metapackVersion, ex);
                            StudioUIUtils.displayIdeaInfoMessage(this.project,
                                    StudioBundle.message("message.TheComponentLibraryCouldNotBeLoaded", metapackVersion));
                            return;
                        }
                    }
                    Module moduleDraft;
                    try {
                        moduleDraft = createModuleDraft(
                                uiContext.getIkasanModule(),
                                metapackVersion,
                                project.getName(),
                                uiContext.getOptions().getPackageName());
                    } catch (StudioBuildException ex) {
                        LOG.warn("STUDIO: Could not create module for " + metapackVersion, ex);
                        StudioUIUtils.displayIdeaInfoMessage(this.project,
                                StudioBundle.message("message.TheModuleCouldNotBeCreated", metapackVersion));
                        return;
                    }
                    ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(this.project, true);
                    componentPropertiesPanel.updateTargetComponent(moduleDraft);
                    PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                            this.project,
                            componentPropertiesPanel,
                            true);
                    if (propertiesPopupDialogue.showAndGet()) {
                        uiContext.setIkasanModule(moduleDraft);
                        if (uiContext.getPalettePanel() != null) {
                            uiContext.getPalettePanel().resetPallette();
                        }
                        StudioUIUtils.displayIdeaInfoMessage(this.project, StudioBundle.message("message.PleaseWaitForIntellijToInitialise"));
                        StudioPsiUtils.refreshCodeFromModel(this.project);
                        disableModuleInitialiseProcess();
                    }
                }
            }
        );
    }

    /**
     * Creates an isolated configuration draft so cancelling the properties dialog cannot mutate the live canvas model.
     */
    static Module createModuleDraft(Module sourceModule, String metapackVersion, String defaultName,
                                    String defaultPackageName) throws StudioBuildException {
        // The initial canvas uses a non-null "dumb" module as a placeholder, so a missing
        // identity is the reliable signal that this is the first module configuration.
        boolean creatingNewModule = sourceModule == null || sourceModule.getIdentity() == null;
        Module draft = sourceModule == null
                ? Module.moduleBuilder().version(metapackVersion).build()
                : sourceModule.cloneToVersion(metapackVersion);
        if (draft == null) {
            throw new StudioBuildException("Could not create a module draft for " + metapackVersion);
        }
        if (draft.getIdentity() == null) {
            draft.setName(defaultName);
            draft.setApplicationPackageName(defaultPackageName);
        }
        if (creatingNewModule) {
            var flowStartupTypeMeta = draft.getComponentMeta().getMetadata("flowStartupType");
            if (flowStartupTypeMeta != null && flowStartupTypeMeta.getDefaultValue() != null) {
                draft.setPropertyValue("flowStartupType", flowStartupTypeMeta.getDefaultValue());
            }
        }
        return draft;
    }

    private JPanel createNewModulePanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(20));

        JBLabel heading = new JBLabel(StudioBundle.message("label.CreateYourIkasanModule"));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() + 2));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(JBUI.scale(8)));

        JBTextArea explanation = new JBTextArea(StudioBundle.message("label.ChooseIkasanVersionExplanation"));
        explanation.setEditable(false);
        explanation.setFocusable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setOpaque(false);
        explanation.setColumns(58);
        explanation.setBorder(JBUI.Borders.empty());
        explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(explanation);
        panel.add(Box.createVerticalStrut(JBUI.scale(12)));

        JBPanel<?> chooserRow = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chooserRow.add(new JBLabel("Ikasan version: "));
        chooserRow.add(metaDataVersionComboBox);
        chooserRow.add(Box.createHorizontalStrut(JBUI.scale(8)));
        chooserRow.add(startButton);
        chooserRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(chooserRow);
        return panel;
    }

    public void enableModuleInitialiseProcess() {
        if (newModulePanel.getParent() != this) {
            this.add(newModulePanel);
        }
    }

    public void disableModuleInitialiseProcess() {
        if (newModulePanel.getParent() == this) {
            this.remove(newModulePanel);
        }
    }

    /**
     * This is called by the mouse click Listener
     * @param me mouse event
     * @param x of the start of the mouse click
     * @param y of the start of the mouse click
     */
    private void mouseClickAction(MouseEvent me, int x, int y) {
        clickStartMouseX = x;
        clickStartMouseY = y;
        if (me.getButton() == MouseEvent.BUTTON1) {
            Pair<Flow, FlowTransportAction> transportClick = getFlowTransportButtonAtXY(x, y);
            if (transportClick != null) {
                Flow flow = transportClick.getLeft();
                FlowTransportAction action = transportClick.getRight();
                String rawState = project.getService(FlowErrorMonitorService.class).getFlowStatuses().getRawState(flow.getIdentity());
                if (FlowTransportAction.isEnabledFor(action, rawState)) {
                    FlowTransportControlAction.fire(project, flow.getIdentity(), action, rawState);
                }
                return;
            }
            Flow errorStatusOwner = getFlowStatusLabelOwnerAtXY(x, y);
            if (errorStatusOwner != null) {
                FlowErrorStates.ErrorInfo error = project.getService(FlowErrorMonitorService.class)
                        .getErrorStates().getError(errorStatusOwner.getIdentity());
                if (error != null) {
                    String report = error.details() != null && !error.details().isBlank()
                            ? error.details() : error.summary();
                    if (report != null && !report.isBlank()) {
                        new ErrorDetailsDialog(project, errorStatusOwner.getIdentity(), report).show();
                    }
                    return;
                }
            }
        }
        FlowElement sendTestMessageOwner = getOwnerForSendTestMessageAtXY(x, y);
        IkasanComponent selectedComponent = getComponentAtXY(x, y);
        dragCandidate = me.getButton() == MouseEvent.BUTTON1 && selectedComponent instanceof FlowElement flowElement
                && !flowElement.getComponentMeta().isInternalEndpoint() ? flowElement : null;

        if (!(selectedComponent instanceof BasicElement ikasanBasicElement)) {
            return;
        }
        // Right-click - popup menus
        if (me.getButton() == MouseEvent.BUTTON3) {
            DesignCanvasContextMenu.showPopupAndNavigateMenu(project, this, me, ikasanBasicElement);
//            if (selectedComponent != null) {
//            } else {
//                DesignCanvasContextMenu.showPopupMenu(project,this, me);
//            }
        } // Left-click on the Send Test Message / Trigger badge
        else if (me.getButton() == MouseEvent.BUTTON1 && sendTestMessageOwner != null) {
            if (IkasanFlowRouteViewHandler.usesTriggerBadge(sendTestMessageOwner)) {
                new TriggerScheduledConsumerAction(project, sendTestMessageOwner)
                        .actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "triggerScheduledConsumer"));
            } else {
                new SendTestMessageAction(project, sendTestMessageOwner)
                        .actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "sendTestMessage"));
            }
        } // Double-click -> go to source
        else if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2 && ! me.isConsumed()) {
            me.consume();
            new NavigateToCodeAction(project, ikasanBasicElement, true)
                    .actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "navigateToCode"));
        } // Single Left-click -> update properties
        else if ((me.getButton() == MouseEvent.BUTTON1) &&
                 (  ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement) != null &&
                    ! ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement).isAlreadySelected()

                 )) {
            editComponent(ikasanBasicElement);
        }
    }

    /**
     * Records that the whole IDE application just regained OS-level focus, so the next canvas click
     * (which may just be the click that brought the window forward) is not treated as a selection click.
     */
    public void notifyApplicationReactivated() {
        applicationReactivatedAtMillis = System.currentTimeMillis();
    }

    /**
     * Place the provided Ikasan Basic Element into the properties panel in edit mode
     * @param basicElement to be edited.
     */
    public void editComponent(BasicElement basicElement) {
        UiContext uiContext = project.getService(UiContext.class);
        setSelectedComponent(basicElement);
        uiContext.setSelectedComponent(basicElement);
        if (basicElement instanceof ExceptionResolver resolver) {
            ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(project, true);
            exceptionResolverPanel.updateTargetComponent(basicElement);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    project,
                    exceptionResolverPanel,
                    false);
            if (propertiesPopupDialogue.showAndGet()) {
                StudioPsiUtils.refreshCodeFromModel(project,
                        resolver.getContainingFlow() != null
                                ? GenerationRequest.flow(resolver.getContainingFlow())
                                : GenerationRequest.full());
                uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
            }
        } else {
            uiContext.getPropertiesTabPanel().updateTargetComponent(basicElement);
            uiContext.getPropertiesPanel().updateTargetComponent(basicElement);
        }
    }

    /**
     * This will be called to redraw the screen after the mouse movement is over, the mouse movement will allow the
     * component to move across the screen but not redraw the connectors, this will ensure all connectors and whole
     * screen is redrawn
     */
    private void mouseReleaseAction(int mouseX, int mouseY){
        if (draggedElement != null) completeComponentMove(mouseX, mouseY);
        dragCandidate = null;
        draggedElement = null;
        dragPoint = null;
        resetContextSensitiveHighlighting();
        if (screenChanged) {
            this.repaint();
            screenChanged = false;
        }
    }

    /**
     * This will be called for every mouse movement on the canvas so use sparingly

     * @param mouseX of the current pointer
     * @param mouseY of the current pointer
     */
    private void mouseMoveAction(int mouseX, int mouseY) {
        Pair<Flow, FlowTransportAction> transportButton = getFlowTransportButtonAtXY(mouseX, mouseY);
        if (transportButton != null) {
            this.setToolTipText(transportButton.getRight().getTooltip());
            return;
        }
        Flow statusLabelOwner = getFlowStatusLabelOwnerAtXY(mouseX, mouseY);
        if (statusLabelOwner != null) {
            FlowErrorStates.ErrorInfo error = project.getService(FlowErrorMonitorService.class)
                    .getErrorStates().getError(statusLabelOwner.getIdentity());
            this.setToolTipText(error != null && error.summary() != null
                    ? error.summary() + " — " + StudioBundle.message("tooltip.ClickForFullErrorDetails") : "");
            return;
        }
        IkasanComponent mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        if (mouseSelectedComponent instanceof Flow && ((Flow) mouseSelectedComponent).getFlowIntegrityStatus() != null) {
            this.setToolTipText(((Flow) mouseSelectedComponent).getFlowIntegrityStatus());
        } else if (mouseSelectedComponent instanceof FlowElement flowElement) {
            this.setToolTipText(buildInputOutputTooltip(flowElement));
        } else {
            this.setToolTipText("");
        }
    }

    /**
     * Compact hover tooltip naming what payload type flows into/out of this component - the same data source
     * as the properties panel's Input:/Output: summary and the upstream type-mismatch warning (see
     * FlowElement#getEffectiveInputTypeDescription / #getEffectiveOutputTypeDescription), so all three can
     * never disagree. Returns "" (never null) when nothing can be said (e.g. an Endpoint marker, which has no
     * payload type of its own), matching this method's existing "no tooltip" convention below.
     */
    private String buildInputOutputTooltip(FlowElement flowElement) {
        String input = flowElement.getEffectiveInputTypeDescription();
        String output = flowElement.getEffectiveOutputTypeDescription();
        if (input == null && output == null) {
            return "";
        }
        StringBuilder tooltip = new StringBuilder("<html>");
        if (input != null) {
            tooltip.append("Input: ").append(StudioUIUtils.escapeHtml(input));
        }
        if (output != null) {
            if (input != null) {
                tooltip.append("<br>");
            }
            tooltip.append("Output: ").append(StudioUIUtils.escapeHtml(output));
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    /**
     * Called by the mouse drag listener
     * @param mouseX at the start of the drag
     * @param mouseY at the start of the drag
     */
    private void mouseDragAction(int mouseX, int mouseY) {
        if (dragCandidate == null) return;
        if (draggedElement == null && Point.distance(clickStartMouseX, clickStartMouseY, mouseX, mouseY) < COMPONENT_DRAG_THRESHOLD) return;
        draggedElement = dragCandidate;
        dragPoint = new Point(mouseX, mouseY);
        screenChanged = true;
        componentDraggedToFlowAction(mouseX, mouseY, draggedElement);
        repaint();
    }

    private void completeComponentMove(int mouseX, int mouseY) {
        IkasanComponent target = getComponentAtXY(mouseX, mouseY);
        Flow targetFlow = target instanceof Flow flow ? flow : target instanceof FlowRoute route ? route.getFlow()
                : target instanceof FlowElement element ? element.getContainingFlow() : null;
        FlowRoute targetRoute = target instanceof FlowRoute route ? route
                : target instanceof FlowElement element ? element.getContainingFlowRoute()
                : targetFlow != null ? targetFlow.getFlowRoute() : null;
        if (targetFlow == null) return;
        int insertionIndex = insertionIndexAtX(targetRoute, mouseX);
        if (target instanceof FlowElement targetElement && targetRoute != null) {
            if (targetElement.getComponentMeta().isConsumer()) {
                // Consumers live on Flow rather than in FlowRoute.flowElements. Dropping on the consumer
                // means immediately after it: index zero of the root route, not the route's end.
                insertionIndex = 0;
            } else {
                int targetIndex = targetRoute.getFlowElements().indexOf(targetElement);
                if (targetIndex >= 0) {
                    AbstractViewHandlerIntellij handler = ViewHandlerCache.getAbstractViewHandler(project, targetElement);
                    insertionIndex = mouseX < handler.getCentrePoint().x ? targetIndex : targetIndex + 1;
                }
            }
        }
        Flow sourceFlow = draggedElement.getContainingFlow();
        FlowElement movingElement = draggedElement;
        int destinationIndex = insertionIndex;
        CommandProcessor.getInstance().executeCommand(project, () -> {
            FlowElementMove.MoveResult result = FlowElementMove.move(
                    movingElement, targetFlow, targetRoute, destinationIndex);
            if (!result.accepted() || !result.changed()) return;
            if (sourceFlow != targetFlow) {
                UserImplementedClassRelocator.relocateIfNeeded(project, getIkasanModule(), movingElement, sourceFlow, targetFlow);
            }
            GenerationRequest request = sourceFlow == targetFlow
                    ? GenerationRequest.flow(targetFlow) : GenerationRequest.full();
            UndoManager.getInstance(project).undoableActionPerformed(new DeleteComponentUndoableAction(
                    project, () -> result.undo(movingElement), () -> result.redo(movingElement), request));
            StudioPsiUtils.refreshCodeFromModelAndCauseRedraw(project, request);
            initialiseAllDimensions = true;
        }, StudioBundle.message("menu.MoveComponent"), null);
    }

    private int insertionIndexAtX(FlowRoute route, int mouseX) {
        if (route == null) return 0;
        List<FlowElement> elements = route.getFlowElements();
        for (int i = 0; i < elements.size(); i++) {
            AbstractViewHandlerIntellij handler = ViewHandlerCache.getAbstractViewHandler(project, elements.get(i));
            if (handler != null && mouseX < handler.getCentrePoint().x) return i;
        }
        return elements.size();
    }

    /**
     * We have clicked on the canvas.
     * If we are on a flow component, set the selected component
     * If we are on a flow but not over a flow component, make the flow the selected component
     * If we are on the  canvas and not any flow, set the module as the selected component
     * @param ikasanBasicElement currently pointed to by the mouse.
     */
    public void setSelectedComponent(BasicElement ikasanBasicElement) {
        Module ikasanModule = getIkasanModule();
        deSelectAllComponentsAndFlows();
        // Set selected
        if (ikasanBasicElement instanceof FlowElement) {
            ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowRoute().getConsumerAndFlowRouteElements().stream())
                    .filter(x -> x.equals(ikasanBasicElement))
                    .forEach(x -> ViewHandlerCache.getAbstractViewHandler(project, x).setAlreadySelected(true));
        } else if (ikasanBasicElement instanceof Flow) {
            ikasanModule.getFlows()
                    .stream()
                    .filter(x -> x.equals(ikasanBasicElement))
                    .forEach(x -> ViewHandlerCache.getAbstractViewHandler(project, x).setAlreadySelected(true));
        } else {
            ViewHandlerCache.getAbstractViewHandler(project, ikasanModule).setAlreadySelected(true);
        }
    }


    /**
     * Ensure everything is deselected.
     */
    private void deSelectAllComponentsAndFlows() {
        Module ikasanModule = getIkasanModule();
        ViewHandlerCache.getAbstractViewHandler(project, ikasanModule).setAlreadySelected(false);
        ikasanModule.getFlows()
                .stream()
                .peek(x -> ViewHandlerCache.getAbstractViewHandler(project, x).setAlreadySelected(false))
                .flatMap(x -> x.getFlowRoute().getConsumerAndFlowRouteElements().stream())
                .filter(x -> ViewHandlerCache.getAbstractViewHandler(project, x).isAlreadySelected())
                .forEach(x -> ViewHandlerCache.getAbstractViewHandler(project, x).setAlreadySelected(false));
    }

    /**
     * Given the x and y coords, return the ikasan elements that reside at that x,y.
     * This will either be an ikasan flows component, an ikasan flow or the whole module.
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public IkasanComponent getComponentAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        IkasanComponent ikasanComponent = null;
        if (ikasanModule != null) {
            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowElementsNoExternalEndPoints().stream())
                    .filter(x -> ViewHandlerCache.getFlowComponentViewHandler(project, x).isComponentAtXY(xpos, ypos))
                    .findFirst()
                    .orElse(null);
        }
        if (ikasanComponent == null) {
            ikasanComponent = getOwnerForEndpointAtXY(xpos, ypos);
        }
        if (ikasanComponent == null) {
            ikasanComponent =  getFlowExceptionResolverAtXY(xpos, ypos);
        }
        if (ikasanComponent == null) {
            ikasanComponent =  getFlowRouteAtXY(xpos, ypos);
        }
        if (ikasanComponent == null) {
            ikasanComponent =  getFlowAtXY(xpos, ypos);
        }
        if (ikasanComponent == null) {
            ikasanComponent = ikasanModule;
        }
        return ikasanComponent;
    }

    /**
     * Given the x and y coords, return the ikasan flow exception resolver that resides at that x,y.
     * or null if no resoler resides at that XY
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public BasicElement getFlowExceptionResolverAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        BasicElement ikasanComponent = null;
        if (ikasanModule != null) {
            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .filter(Flow::hasExceptionResolver)
                    .filter(x -> ViewHandlerCache.getAbstractViewHandler(project, x.getExceptionResolver()).getLeftX() <= xpos &&
                            ViewHandlerCache.getAbstractViewHandler(project, x.getExceptionResolver()).getRightX() >= xpos &&
                            ViewHandlerCache.getAbstractViewHandler(project, x.getExceptionResolver()).getTopY() <= ypos &&
                            ViewHandlerCache.getAbstractViewHandler(project, x.getExceptionResolver()).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);

            if (ikasanComponent != null) {
                ikasanComponent = ((Flow) ikasanComponent).getExceptionResolver();
            }
        }

        return ikasanComponent;
    }

    /**
     * Given x,y coords, check whether the click landed on an external endpoint icon.
     * If so, return the owning consumer or producer FlowElement so its properties are shown.
     * Returns null if no endpoint was hit.
     */
    private FlowElement getOwnerForEndpointAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        if (ikasanModule == null) {
            return null;
        }
        for (Flow flow : ikasanModule.getFlows()) {
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            if (flowViewHandler != null) {
                FlowElement owner = flowViewHandler.getOwnerForEndpointAtXY(xpos, ypos);
                if (owner != null) {
                    return owner;
                }
            }
        }
        return null;
    }

    /**
     * Given x,y coords, check whether the click landed on a Send Test Message badge (rendered centred above
     * an external endpoint icon). If so, return the owning Consumer FlowElement.
     * Returns null if no badge was hit.
     */
    private FlowElement getOwnerForSendTestMessageAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        if (ikasanModule == null) {
            return null;
        }
        for (Flow flow : ikasanModule.getFlows()) {
            IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            if (flowViewHandler != null) {
                FlowElement owner = flowViewHandler.getOwnerForSendTestMessageAtXY(xpos, ypos);
                if (owner != null) {
                    return owner;
                }
            }
        }
        return null;
    }

    /**
     * Given the x and y coords, return the ikasan flow that resides at that x,y.
     *
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public BasicElement getFlowAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        BasicElement ikasanComponent = null;
        if (ikasanModule != null) {

            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .filter(x -> ViewHandlerCache.getAbstractViewHandler(project, x).getLeftX() <= xpos && ViewHandlerCache.getAbstractViewHandler(project, x).getRightX() >= xpos && ViewHandlerCache.getAbstractViewHandler(project, x).getTopY() <= ypos && ViewHandlerCache.getAbstractViewHandler(project, x).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
        }
        return ikasanComponent;
    }

    /**
     * Given the x and y coords, return the ikasan flow route that resides at that x,y.
     * This is essentially the space between the components in a flow
     *
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public IkasanComponent getFlowRouteAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        IkasanComponent ikasanComponent = null;
        IkasanFlowRouteViewHandler ikasanFlowRouteViewHandler = ikasanModule.getFlows()
                .stream()
                .map(x -> ViewHandlerCache.getAbstractViewHandler(project, x))
                .map(x -> ((IkasanFlowViewHandler) x))
                .flatMap(x -> x.getFlowRouteViewHandler().getAllFlowRouteViewHandlers(new ArrayList<>(), x.getFlowRouteViewHandler()).stream())
                .filter(x -> x.getLeftX() <= xpos && x.getRightX() >= xpos && x.getTopY() <= ypos && x.getBottomY() >= ypos)
                .findFirst()
                .orElse(null);
        if (ikasanFlowRouteViewHandler != null) {
            ikasanComponent = ikasanFlowRouteViewHandler.getFlowRoute();
        }
        return ikasanComponent;
    }

    /**
     * The transferHandler has indicated we are over a flow, decide how we will highlight that flow
     * Return true if its OK for the component to be dropped
     */
    public boolean componentDraggedToFlowAction(int mouseX, int mouseY, final BasicElement ikasanBasicElement) {
        boolean okToAdd = false;
        if (ikasanBasicElement != null) {
            final IkasanComponent targetElement = getComponentAtXY(mouseX, mouseY);

            Flow targetFlow = null;
            FlowRoute targetFlowRoute = null;
            if (targetElement instanceof Flow) {
                targetFlow = (Flow)targetElement;
            } else if (targetElement instanceof FlowRoute) {
                targetFlowRoute = (FlowRoute)targetElement;
                targetFlow = targetFlowRoute.getFlow();
            } else if (targetElement instanceof FlowElement) {
                targetFlow = ((FlowElement)targetElement).getContainingFlow();
                targetFlowRoute = ((FlowElement)targetElement).getContainingFlowRoute();
            } else if (targetElement instanceof Module) {
                if (ikasanBasicElement instanceof Flow) {
                    okToAdd = true;
                }
            }

            if ((ikasanBasicElement.getComponentMeta().isDebug() && targetElement instanceof FlowElement && !((FlowElement)targetElement).getComponentMeta().isProducer()) ||
                (!ikasanBasicElement.getComponentMeta().isDebug() && (targetFlowRoute != null || targetFlow != null))) {
                // Enabled when trcing UI drop issues
                //LOG.info("Taget element was " + targetElement);

                String issue = "";
                if (targetFlowRoute != null) {
                    issue = targetFlowRoute.issueCausedByAdding(ikasanBasicElement.getComponentMeta());
                }
                if (targetFlow != null) {
                    issue += targetFlow.issueCausedByAdding(ikasanBasicElement.getComponentMeta(), targetFlowRoute);
                    IkasanFlowViewHandler ikasanFlowViewHandler = ViewHandlerCache.getFlowViewHandler(project, targetFlow);
                    if (issue.isEmpty()) {
                        okToAdd = true;
                        if (ikasanFlowViewHandler.setFlowReceptiveMode()) {
                            this.repaint();
                        }
                    } else {
                        if (ikasanFlowViewHandler.setFlowlWarningMode(mouseX, mouseY, issue)) {
                            this.repaint();
                        }
                    }
                }
            } else if (okToAdd) {
                // e.g. a new Flow being dragged onto empty canvas - a valid drop, nothing to warn about.
                resetContextSensitiveHighlighting();
            } else {
                // A drag is in progress but the mouse isn't over anywhere it could be dropped right now.
                setAllFlowsNotDropTargetMode();
            }
        }
        return okToAdd;
    }

    public void resetContextSensitiveHighlighting() {
        Module ikasanModule = getIkasanModule();
        boolean redrawNeeded = false;
        for (Flow flow : ikasanModule.getFlows()) {
            if (((IkasanFlowViewHandler) ViewHandlerCache.getAbstractViewHandler(project, flow)).setFlowNormalMode()) {
                redrawNeeded = true;
            }
        }
        if (redrawNeeded) {
            this.repaint();
        }
    }

    /**
     * A drag is in progress but the mouse is not currently over a valid drop target, so every flow's border
     * shows red until the user drags over a flow that can accept the component (or the drag ends).
     */
    private void setAllFlowsNotDropTargetMode() {
        Module ikasanModule = getIkasanModule();
        boolean redrawNeeded = false;
        for (Flow flow : ikasanModule.getFlows()) {
            if (((IkasanFlowViewHandler) ViewHandlerCache.getAbstractViewHandler(project, flow)).setFlowNotDropTargetMode()) {
                redrawNeeded = true;
            }
        }
        if (redrawNeeded) {
            this.repaint();
        }
    }

    /**
     * Given
     * @param xpos of element
     * @param ypos of element
     * @return ikasan elements to the left or right (or both) within reasonable bounds
     */
    public Pair<FlowElement, FlowElement> getSurroundingComponents(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        Pair<FlowElement, FlowElement> surroundingComponents = new Pair<>();
        Point dragged = new Point(xpos, ypos);
        Pair<Integer, Integer> proximityDetect = IkasanFlowComponentViewHandler.getProximityDetect();

        if (ikasanModule != null) {
            for (Flow flow : ikasanModule.getFlows()) {
                for (FlowElement ikasanFlowComponent : flow.getFlowElementsNoExternalEndPoints()) {
                    Proximity draggedToComponent = Proximity.getRelativeProximity(dragged, ViewHandlerCache.getAbstractViewHandler(project, ikasanFlowComponent).getCentrePoint(), proximityDetect);
                    if (draggedToComponent == Proximity.LEFT) {
                        surroundingComponents.setLeft(ikasanFlowComponent);
                    } else if (draggedToComponent == Proximity.RIGHT || draggedToComponent == Proximity.CENTER) {
                        surroundingComponents.setRight(ikasanFlowComponent);
                    }
                }
            }
        }
        return surroundingComponents;
    }

    /**
     * We must be on a flow, with a flow component 'in hand', lets see if we can add it to the flow.
     * @param x location of the mouse
     * @param y location of the mouse
     * @param ikasanComponentType to be added
     * @return true of we managed to add the component.
     */
    public boolean requestToAddComponent(int x, int y, ComponentMeta ikasanComponentType) {
        UiContext uiContext = project.getService(UiContext.class);
        Module ikasanModule = getIkasanModule();
        if (x >= 0 && y >= 0) {
            IkasanComponent targetElement = getComponentAtXY(x,y);
            IkasanObject newComponent;
            if (targetElement instanceof FlowElement || targetElement instanceof Flow || targetElement instanceof FlowRoute) {
                Flow containingFlow;
                FlowRoute containingFlowRoute;
                if (targetElement instanceof Flow) {
                    containingFlow = (Flow)targetElement;
                    containingFlowRoute = containingFlow.getFlowRoute();
                } else if (targetElement instanceof FlowRoute) {
                    containingFlowRoute = (FlowRoute)targetElement;
                    containingFlow = containingFlowRoute.getFlow();
                } else {
                    containingFlow = ((FlowElement)targetElement).getContainingFlow();
                    containingFlowRoute = ((FlowElement)targetElement).getContainingFlowRoute();
                }

                // Defensive
                if (containingFlow == null || containingFlowRoute == null || containingFlow.getFlowRoute() == null) {
                    LOG.warn("STUDIO: WARNING: despite thinking we are over a flow, it appears either of the following were null which is not expected " +
                            "containingFlow " + containingFlow + " containingFlowRoute " + containingFlowRoute + " containingFlow.getFlowRoute() null " +
                        " x " + x + " y " + y);
                    resetContextSensitiveHighlighting();
                    return false;
                }

                //  If the add is not allowed, return false.
                if (! containingFlowRoute.isValidToAdd(ikasanComponentType) || !containingFlow.getFlowRoute().isValidToAdd(ikasanComponentType)) {
                    resetContextSensitiveHighlighting();
                    return false;
                }

                newComponent = null;
                try {
                    newComponent = createViableFlowComponent(ikasanComponentType, containingFlow, containingFlowRoute, x, y);
                } catch (Exception ex) {
                    // Any exceptions raised here were silently handled, now exposed at least as logs
                    LOG.warn("STUDIO: ERROR: Intercept silent popup box failure, " + ex + " trace: " + Arrays.toString(ex.getStackTrace()));
                }
                if (newComponent != null) {
                    if (newComponent instanceof ExceptionResolver) {
                        containingFlow.setExceptionResolver((ExceptionResolver) newComponent);
                    } else {
                        ((FlowElement) newComponent).defaultUnsetMandatoryProperties();
                        insertNewComponentBetweenSurroundingPair(containingFlow, containingFlowRoute, (FlowElement) newComponent, x, y);
                        if (newComponent.getComponentMeta().isRouter()) {
                            syncChildRoutesForRouter((FlowElement) newComponent);
                        }
                    }
                } else {
                    return false;
                }

            } else {
                // The targetElement was the module, so we must be adding a new flow.

                try {
                    newComponent = createViableComponent(Flow
                            .flowBuilder()
                            .metapackVersion(uiContext.getIkasanModule().getMetaVersion())
                            .build());
                } catch (StudioBuildException e) {
                    StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ThereWasAProblemTryingToGetMetaPackInfo", e.getMessage()));
                    return false;
                }
                if (newComponent != null) {
                    ikasanModule.addFlow((Flow) newComponent);
                } else {
                    return false;
                }
            }
            GenerationRequest generationRequest = newComponent instanceof Flow addedFlow
                    ? GenerationRequest.moduleStructure(addedFlow)
                    : GenerationRequest.flow(((FlowElement) newComponent).getContainingFlow());
            StudioPsiUtils.refreshCodeFromModel(project, generationRequest);
            uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());

            initialiseAllDimensions = true;
            this.repaint();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creation for a flow component
     * @param ikasanComponentType to create
     * @param containingFlow that will hold this component
     * @param x drop location, used only to suggest a starting value for the new component's declared input
     *          type (see {@link #applySuggestedInputTypeFromUpstream}) before its properties dialog is shown -
     *          the component is not actually positioned in the flow until insertNewComponentBetweenSurroundingPair
     * @param y drop location, see x
     * @return the fully populated component or null if the action was cancelled.
     */
    private FlowElement createViableFlowComponent(ComponentMeta ikasanComponentType, Flow containingFlow, FlowRoute containingFlowFoute, int x, int y) {
        UiContext uiContext = project.getService(UiContext.class);
        FlowElement newComponent = null;
        try {
            newComponent = FlowElementFactory.createFlowElement(uiContext.getIkasanModule().getMetaVersion(), ikasanComponentType, containingFlow, containingFlowFoute, null);
        } catch (StudioBuildException e) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ThereWasAProblemTryingToGetMetaPackInfo", e.getMessage()));
            return newComponent;
        }
        applySuggestedInputTypeFromUpstream(newComponent, containingFlow, x, y);
        if (ikasanComponentType.isExceptionResolver()) {
            return (FlowElement)createExceptionResolver((ExceptionResolver)newComponent);
        } else {
            return (FlowElement)createViableComponent(newComponent);
        }
    }

    /**
     * Pre-fills newComponent's declared input-type property (see ComponentMeta#getExpectedInputTypeProperty -
     * 'fromType' by convention, e.g. Converter/Broker/Splitter/Filter/Router/Producer, or a differently-named
     * property such as Object To XML String Converter's 'objectClass') with whatever the nearest upstream
     * payload-bearing component at the drop point declares as its effective output type (see
     * FlowElement#getEffectiveOutputTypeDescription) - a live 'toType' property for most components, but also a
     * fixed metadata constant for components with no such property of their own (e.g. a Consumer's
     * producedOutputType). Routers, Filters and Debug breakpoints are skipped over since none of them change the
     * payload's type (see Flow#skipNonPayloadBearingElements).
     * -
     * This is only a starting suggestion shown (and freely editable) in the properties dialog that follows -
     * left alone whenever newComponent has no such property, or there is nothing upstream to suggest from (e.g.
     * dropped right after a Consumer, or right after a component with no declared output type of its own).
     * @param newComponent just created, not yet positioned in the flow
     * @param containingFlow the new component is being added to
     * @param x drop location
     * @param y drop location
     */
    private void applySuggestedInputTypeFromUpstream(FlowElement newComponent, Flow containingFlow, int x, int y) {
        if (newComponent == null || newComponent.getComponentMeta() == null) {
            return;
        }
        String propertyName = newComponent.getComponentMeta().getEffectiveInputTypePropertyName();
        ComponentPropertyMeta propertyMeta = newComponent.getComponentMeta().getMetadata(propertyName);
        if (propertyMeta == null || propertyMeta.isHiddenProperty()) {
            // No such property (most components), or it's hidden from the user (e.g. Debug's fromType, always
            // java.lang.Object by design) - nothing sensible to suggest either way.
            return;
        }
        Pair<FlowElement, FlowElement> surroundingComponents = getSurroundingComponents(x, y);
        FlowElement upstream = containingFlow.skipNonPayloadBearingElements(surroundingComponents.getLeft());
        if (upstream == null) {
            return;
        }
        String upstreamOutputType = upstream.getEffectiveOutputTypeDescription();
        if (upstreamOutputType != null && !upstreamOutputType.isBlank()) {
            newComponent.setPropertyValue(propertyName, upstreamOutputType);
        }
    }

    /**
     * Create the popup properties panel for a new component
     * @param newComponent to be included in panel
     * @return the populated component or null if the action was cancelled.
     */
    private BasicElement createViableComponent(BasicElement newComponent) {
        UiContext uiContext = project.getService(UiContext.class);
        // Now this is a serious components, ensure any property with tag placeholder are updated to real values
        if (newComponent instanceof FlowElement newFlowComponent) {
            StudioBuildUtils.substituteAllPlaceholderInPascalCase(uiContext.getIkasanModule(), newFlowComponent.getContainingFlow(), newFlowComponent);
            if (newComponent.getComponentMeta().isDebug()) {
                newFlowComponent.defaultUnsetMandatoryProperties();
            }
        }


        if (newComponent.hasUnsetMandatoryProperties()) {
            // Add new component
            ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(project, true);
            componentPropertiesPanel.updateTargetComponent(newComponent);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    project,
                    componentPropertiesPanel,
                    false);
            if (! propertiesPopupDialogue.showAndGet()) {
                // i.e. cancel.
                newComponent = null;
            }
        }
        return newComponent;
    }

    /**
     * Create the popup properties panel for a new component
     * @param newExceptionResolver to be included in panel
     * @return the populated component or null if the action was cancelled.
     */
    private BasicElement createExceptionResolver(ExceptionResolver newExceptionResolver) {
        if (    newExceptionResolver.hasUnsetMandatoryProperties() ||
                newExceptionResolver.getIkasanExceptionResolutionMap() == null ||
                newExceptionResolver.getIkasanExceptionResolutionMap().isEmpty() ) {

            ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(project, true);
            exceptionResolverPanel.updateTargetComponent(newExceptionResolver);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    project,
                    exceptionResolverPanel,
                    false);
            if (! propertiesPopupDialogue.showAndGet()) {
                // i.e. cancel.
                newExceptionResolver = null;
            }
        }
        return newExceptionResolver;
    }

    /**
     * Now we have validated it is OK to add the component, insert it at the drag point
     * @param containingFlow holding the new component
     * @param ikasanFlowComponent to be added
     * @param x location of the drop
     * @param y location of the drop
     */
    private void insertNewComponentBetweenSurroundingPair(Flow containingFlow, FlowRoute containingFlowRoute, FlowElement ikasanFlowComponent, int x, int y) {
        if (ikasanFlowComponent.getComponentMeta().isConsumer()) {
            containingFlow.setConsumer(ikasanFlowComponent);
        } else {
            // insert new component between surrounding pair
            Pair<FlowElement, FlowElement> surroundingComponents = getSurroundingComponents(x, y);
            // No routes currently exist
            if (containingFlowRoute == null && !containingFlow.anyFlowRouteHasComponents(containingFlow.getFlowRoute())) {
                List<FlowElement> components = containingFlow.getFlowRoute().getFlowElements() ;
                components.add(ikasanFlowComponent);
            } else {

                FlowRoute targetRoute = containingFlowRoute;
                if (targetRoute == null && surroundingComponents.getRight() != null) {
                    targetRoute = containingFlow.getFlowRouteContaining(containingFlow.getFlowRoute(), surroundingComponents.getRight());
                }
                if (targetRoute == null && surroundingComponents.getLeft() != null) {
                    targetRoute = containingFlow.getFlowRouteContaining(containingFlow.getFlowRoute(), surroundingComponents.getLeft());
                }

                // Prefer the LEFT (preceding) component for naming, matching insertDebugComponentAfter's
                // convention (a Debug names itself after the component it follows, e.g. "MytDebug" for a Debug
                // dropped right after "myt") - RIGHT is only a fallback for the edge case of dropping a Debug
                // at the very start of a route, before anything else exists to its left.
                FlowElement debugNamingAnchor = surroundingComponents.getLeft() != null
                        ? surroundingComponents.getLeft() : surroundingComponents.getRight();
                if (ikasanFlowComponent.getComponentMeta().isDebug() && debugNamingAnchor != null) {
                    // The LEFT neighbour picked above may itself be a Debug (dropping a second Debug right
                    // after an existing one) - name after the real component the chain is actually debugging,
                    // not after the other Debug (which would otherwise compound into e.g. "MytDebugDebug").
                    FlowElement realAnchor = resolveRealDebugAnchor(debugNamingAnchor, targetRoute, containingFlow);
                    if (realAnchor == null) {
                        realAnchor = debugNamingAnchor;
                    }
                    assignDebugIdentityAndClassName(ikasanFlowComponent, realAnchor.getIdentity(), containingFlow);
                }
                if (targetRoute != null) {
                    List<FlowElement> components = targetRoute.getFlowElements();
                    int numberOfComponents = components.size();

                    // The Consumer is never a member of components (it's held on the Flow, not the FlowRoute),
                    // so it can never be matched by the loop below regardless of which side it resolved to -
                    // insert at the front instead. Which side it resolves to depends on exactly where within
                    // the Consumer's icon the drop landed, so both sides must be checked.
                    boolean droppedAdjacentToConsumer =
                            (surroundingComponents.getRight() != null && surroundingComponents.getRight().getComponentMeta().isConsumer()) ||
                            (surroundingComponents.getLeft() != null && surroundingComponents.getLeft().getComponentMeta().isConsumer());

                    if (numberOfComponents == 0 || droppedAdjacentToConsumer) {
                        components.add(0, ikasanFlowComponent);
                    } else {
                        boolean inserted = false;
                        for (int ii = 0; ii < numberOfComponents; ii++) {
                            if (components.get(ii).equals(surroundingComponents.getRight()) ||
                                    ((components.get(ii).equals(surroundingComponents.getLeft())) && surroundingComponents.getLeft().getComponentMeta().isProducer())) {
                                // A Router Endpoint (isInternalEndpoint) always anchors the very start of its
                                // route - it's the branch's connection point back to the router, so nothing may
                                // ever land before it here, however the left/right proximity resolved. Without
                                // this, a drop that misjudges which side of a freshly-created branch's endpoint
                                // it landed on inserts the new component before the endpoint, drawing it before
                                // the "ball" instead of after (only a full model.json reload self-heals that,
                                // since ModuleDeserializer always rebuilds a branch with its endpoint first).
                                int insertAt = components.get(ii).getComponentMeta().isInternalEndpoint() ? ii + 1 : ii;
                                components.add(insertAt, ikasanFlowComponent);
                                inserted = true;
                                break;
                            } else if (components.get(ii).equals(surroundingComponents.getLeft())) {
                                components.add(ii + 1, ikasanFlowComponent);
                                inserted = true;
                                break;
                            }
                        }
                        if (!inserted) {
                            LOG.warn("STUDIO: Dropped " + ikasanFlowComponent.getIdentity() + " at (" + x + "," + y +
                                    ") but could not resolve an insertion point (left=" +
                                    (surroundingComponents.getLeft() != null ? surroundingComponents.getLeft().getIdentity() : "null") + ", right=" +
                                    (surroundingComponents.getRight() != null ? surroundingComponents.getRight().getIdentity() : "null") +
                                    "); nothing was added to the flow.");
                        }
                    }
                } else {
                    LOG.warn("STUDIO: Could not resolve a target FlowRoute to insert " + ikasanFlowComponent.getIdentity() + " into at (" + x + "," + y + "); nothing was added.");
                }
            }
        }
    }

    /**
     * A router's childRoutes only ever get built from its routeNames property during a full model.json reload
     * (see ModuleDeserializer#addNewRoutesForRouter) - a live drag-and-drop add never does that, so without
     * this a freshly dropped router has no branches to drop a Producer (or anything else) into, and the canvas
     * would wrongly report "cannot have a router AND a producer" against the router's own containing route.
     * -
     * A router must always be the LAST element of the FlowRoute it lives in (see the comment in
     * ModuleDeserializer#buildRouteTree: "the router always marks the end of a route") - both rendering
     * (IkasanFlowRouteViewHandler lays out a route's own flowElements in one straight line, then starts its
     * childRoutes from wherever that line ends up) and code generation assume this. Dropping a router in the
     * MIDDLE of an existing chain (e.g. just before an already-placed Producer) only satisfies half of that:
     * insertNewComponentBetweenSurroundingPair splices the router into the flat list at the drop point, but
     * leaves whatever already followed it still sitting after it in that SAME list - so any such trailing
     * elements are relocated into the router's own first new branch below, matching the drag-and-drop's own
     * intent (auto-attach the existing downstream chain onto the router's first route, leaving the rest
     * dangling for the user to complete) instead of them staying wrongly attached past the router itself.
     * @param router just added to the canvas
     */
    private void syncChildRoutesForRouter(FlowElement router) {
        FlowRoute containingFlowRoute = router.getContainingFlowRoute();
        if (containingFlowRoute == null) {
            LOG.warn("STUDIO: SERIOUS: Newly added router " + router.getIdentity() + " had no containing FlowRoute, could not sync its child routes.");
            return;
        }
        try {
            containingFlowRoute.syncChildRoutesForRouter(getIkasanModule().getMetaVersion(), router);
        } catch (StudioBuildException se) {
            LOG.warn("STUDIO: A studio exception was raised while syncing child routes for router " + router.getIdentity() + ", please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
            return;
        }
        moveTrailingElementsIntoRoutersFirstBranch(containingFlowRoute, router);
    }

    /**
     * See syncChildRoutesForRouter above - if the router was dropped in the middle of an existing chain rather
     * than at its own route's very end, whatever already followed it there is moved into the router's first
     * branch (right after that branch's own Router Endpoint marker), instead of being left dangling past the
     * router in the same flat list.
     */
    private void moveTrailingElementsIntoRoutersFirstBranch(FlowRoute containingFlowRoute, FlowElement router) {
        List<FlowElement> components = containingFlowRoute.getFlowElements();
        int routerIndex = components.indexOf(router);
        if (routerIndex < 0 || routerIndex == components.size() - 1) {
            // Router is already the last element (the common case - dropped at the end of the chain, or
            // standalone) - nothing to relocate.
            return;
        }
        List<FlowElement> trailingElements = new ArrayList<>(components.subList(routerIndex + 1, components.size()));
        components.subList(routerIndex + 1, components.size()).clear();

        if (containingFlowRoute.getChildRoutes() == null || containingFlowRoute.getChildRoutes().isEmpty()) {
            LOG.warn("STUDIO: SERIOUS: Router " + router.getIdentity() + " had trailing elements to relocate but no child routes were created for it.");
            return;
        }
        FlowRoute firstBranch = containingFlowRoute.getChildRoutes().get(0);
        for (FlowElement trailingElement : trailingElements) {
            trailingElement.setContainingFlowRoute(firstBranch);
            firstBranch.getFlowElements().add(trailingElement);
        }
    }

    /**
     * Inserts a Debug component immediately after the given target, so its generated debug() method
     * can be used as an IntelliJ breakpoint to inspect the payload the target just produced.
     * Reuses the same component-creation machinery as drag-and-drop ({@link #createViableFlowComponent})
     * rather than the x/y-proximity-based {@link #insertNewComponentBetweenSurroundingPair}, since there
     * is no drag position to work from here.
     * @param targetElement the component to add a Debug component after
     * @return the inserted Debug FlowElement, or null if it could not be added (invalid target,
     *         missing Debug component meta, or the user cancelled the properties popup)
     */
    public FlowElement insertDebugComponentAfter(FlowElement targetElement) {
        if (targetElement.getComponentMeta().isProducer() || targetElement.getComponentMeta().isDebug()) {
            LOG.warn("STUDIO: Debug cannot be added after a Producer (it is the last component in a flow) or another Debug component: " + targetElement.getIdentity());
            return null;
        }

        Flow containingFlow = targetElement.getContainingFlow();
        FlowRoute containingFlowRoute = targetElement.getContainingFlowRoute();
        if (containingFlow == null || containingFlowRoute == null) {
            return null;
        }

        ComponentMeta debugMeta;
        try {
            debugMeta = IkasanComponentLibrary.getIkasanComponents(getIkasanModule().getMetaVersion()).values().stream()
                    .filter(ComponentMeta::isDebug)
                    .findFirst()
                    .orElse(null);
        } catch (StudioBuildException e) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ThereWasAProblemTryingToGetMetaPackInfo", e.getMessage()));
            return null;
        }
        if (debugMeta == null) {
            LOG.warn("STUDIO: No Debug component meta found for this metapack version");
            return null;
        }

        // No drag position exists for this programmatic insertion path (see class javadoc above) - harmless,
        // since Debug's fromType is hidden and skipped by applySuggestedInputTypeFromUpstream regardless.
        FlowElement debugComponent = createViableFlowComponent(debugMeta, containingFlow, containingFlowRoute, 0, 0);
        if (debugComponent == null) {
            // User cancelled the properties popup, or an error was already reported.
            return null;
        }

        assignDebugIdentityAndClassName(debugComponent, targetElement.getIdentity(), containingFlow);

        List<FlowElement> components = containingFlowRoute.getFlowElements();
        if (targetElement.getComponentMeta().isConsumer()) {
            // The consumer is not itself a member of the route's element list (it's held on the Flow),
            // so "after the consumer" is always index 0 of the route.
            components.add(0, debugComponent);
        } else {
            int targetIndex = components.indexOf(targetElement);
            if (targetIndex < 0) {
                return null;
            }
            components.add(targetIndex + 1, debugComponent);
        }

        StudioPsiUtils.refreshCodeFromModel(project, GenerationRequest.flow(containingFlow));
        initialiseAllDimensions = true;
        repaint();
        return debugComponent;
    }

    /**
     * Sets a Debug component's identity (derived from the component it's attached to) and, from that, its
     * generated userImplementedClassName - called from both debug-insertion paths (context-menu
     * insertDebugComponentAfter and drag-and-drop insertNewComponentBetweenSurroundingPair).
     * -
     * Must derive the classname from the PRISTINE "__module__flow__componentDebug" template read straight off
     * the component-TYPE metadata (getComponentMeta().getAllowableProperties()), not from this FlowElement's
     * own current property value/meta. By the time this runs, createViableComponent's generic
     * StudioBuildUtils#substituteAllPlaceholderInPascalCase call has already substituted "__component" once,
     * using whatever placeholder/still-default identity the component had at THAT point - i.e. before this
     * method gives it its real, per-anchor identity below. Every Debug component created in the same flow hits
     * that generic substitution with the same not-yet-real identity, so without re-deriving from the pristine
     * template here, they'd all resolve to the exact same userImplementedClassName and silently overwrite one
     * another's generated file - only ever leaving one Debug class behind, no matter how many were added.
     */
    private void assignDebugIdentityAndClassName(FlowElement debugComponent, String anchorIdentity, Flow containingFlow) {
        // Multiple Debugs can legitimately end up wanting the same base name - e.g. two Debugs dropped right
        // after the same real component (via the context menu twice), or several chained one after another
        // (resolveRealDebugAnchor collapses those all onto the same real anchor). Appending an ascending
        // integer (2, 3, ...) keeps each one uniquely identified/generated instead of silently colliding.
        String candidateIdentity = anchorIdentity + "Debug";
        int suffix = 2;
        while (isIdentityTakenByAnotherComponent(candidateIdentity, debugComponent, containingFlow)) {
            candidateIdentity = anchorIdentity + "Debug" + suffix;
            suffix++;
        }
        debugComponent.setIdentity(candidateIdentity);
        String pristineClassnameTemplate = (String) debugComponent.getComponentMeta()
                .getAllowableProperties().get(USER_IMPLEMENTED_CLASS_NAME).getDefaultValue();
        String substitutedClassname = substitutePlaceholderInPascalCase(getIkasanModule(), containingFlow, debugComponent, pristineClassnameTemplate);
        debugComponent.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, substitutedClassname);
    }

    private boolean isIdentityTakenByAnotherComponent(String candidateIdentity, FlowElement debugComponent, Flow containingFlow) {
        return containingFlow.ftlGetConsumerAndFlowElements().stream()
                .anyMatch(sibling -> sibling != debugComponent && candidateIdentity.equals(sibling.getIdentity()));
    }

    /**
     * Walks backward from candidate through any chain of Debug components already attached one after another,
     * to find the nearest REAL (non-Debug) component - a Debug should always be named after what it's actually
     * debugging, not after another Debug it happens to be chained onto. Falls back to the flow's consumer if
     * the chain runs all the way back to the start of the route.
     * @return the real component, or null if candidate itself is null or the route can't be resolved.
     */
    private FlowElement resolveRealDebugAnchor(FlowElement candidate, FlowRoute route, Flow containingFlow) {
        FlowElement current = candidate;
        while (current != null && current.getComponentMeta().isDebug()) {
            current = previousComponentInRoute(current, route, containingFlow);
        }
        return current;
    }

    private FlowElement previousComponentInRoute(FlowElement component, FlowRoute route, Flow containingFlow) {
        if (route == null) {
            return null;
        }
        List<FlowElement> components = route.getFlowElements();
        int index = components.indexOf(component);
        if (index > 0) {
            return components.get(index - 1);
        }
        // Either the very first element in the route, or not found in this route at all (e.g. it's the
        // consumer, which is never a member of route.getFlowElements()) - either way, the consumer is the
        // correct "previous" component to fall back to.
        return containingFlow.getConsumer();
    }

    /**
     * This overrides the parent JPanel paintComponent.
     */
    @Override
    public void paintComponent(Graphics g) {
        Module ikasanModule = getIkasanModule();
        if (ikasanModule != null && ikasanModule.isInitialised()) {
            disableModuleInitialiseProcess();
        } else {
            enableModuleInitialiseProcess();
        }
        if (ikasanModule != null) {
            AbstractViewHandlerIntellij moduleViewHandler = ViewHandlerCache.getAbstractViewHandler(project, ikasanModule);
            // If it was null, we have already logged
            if (moduleViewHandler != null) {
                if (initialiseAllDimensions) {
                    moduleViewHandler.initialiseDimensions(g, 0, 0, this.getWidth(), this.getHeight());
                    initialiseAllDimensions = false;
                }
                // Typically, when pasting in a model.json from an Ikasan module, some studio-specific properties might be absent.
                // Note this is the main paint loop, so DO NOT pop up any dialogues here.
                if (ikasanModule.hasUnsetMandatoryProperties()) {
                    String msg = "The model has the following unset mandatory properties [" + ikasanModule.listUnsetMandatoryProperties() +
                            "] setting to defaults";
                    ikasanModule.defaultUnsetMandatoryProperties();
                    if (ikasanModule.hasUnsetMandatoryProperties()) {
                        msg += " failed. Please add these manually and 'Refresh' design.";
                    }
                    LOG.info("STUDIO: WARN: " + msg);
                }
                int newWidth = moduleViewHandler.getWidth();
                int newHeight = moduleViewHandler.getHeight();
                this.setPreferredSize(JBUI.size(newWidth, newHeight));
                revalidate();
                super.paintComponent(g);
                moduleViewHandler.paintComponent(this, g, -1, -1);
                paintDraggedComponentGhost(g);
                paintJmsDestinationConnectors(g, ikasanModule);
                paintTestMailServerNode(g, ikasanModule);
                updateFlowErrorFlashState(ikasanModule);
                paintFlowTransportControls(g, ikasanModule);
            }
        }
        paintGettingStartedHint(g, ikasanModule);
        updateRunModuleButtonState(ikasanModule);
    }

    private void paintDraggedComponentGhost(Graphics graphics) {
        if (draggedElement == null || dragPoint == null || !(graphics instanceof Graphics2D)) return;
        IkasanFlowComponentViewHandler handler = ViewHandlerCache.getFlowComponentViewHandler(project, draggedElement);
        if (handler == null || handler.getCanvasIcon() == null) return;
        Graphics2D ghost = (Graphics2D) graphics.create();
        try {
            ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            int x = dragPoint.x - handler.getCanvasIcon().getIconWidth() / 2;
            int y = dragPoint.y - handler.getCanvasIcon().getIconHeight() / 2;
            handler.getCanvasIcon().paintIcon(this, ghost, x, y);
        } finally {
            ghost.dispose();
        }
    }

    // Deliberately its own fixed colour (light/dark variants), not sourced via ThemeAwareColors - those look
    // up generic UIManager keys first, which are non-null in virtually every theme and so silently shadow a
    // custom accent colour with a neutral one (see StudioUIUtils#ATTENTION_PULSE_COLOR's own comment for the
    // same reasoning).
    private static final JBColor JMS_CONNECTOR_COLOR = new JBColor(new Color(70, 130, 180), new Color(120, 170, 220));
    private static final float[] JMS_CONNECTOR_DASH = {6f, 4f};
    private static final int JMS_CONNECTOR_GUTTER_MARGIN = 30;
    private static final int JMS_CONNECTOR_STANDOFF = 20;
    private static final int JMS_CONNECTOR_GAP_CLEARANCE = 10;
    private static final int JMS_CONNECTOR_STAGGER = 14;
    private static final int JMS_CONNECTOR_ARROW_SIZE = 6;

    /**
     * Draws an ESB-style orthogonal connector between a JMS Producer and a JMS Consumer, in different flows of
     * this module, that reference the same destination - see {@link JmsFlowConnections#findMatchingLinks}. All
     * flows have already had {@code initialiseDimensions} run for this paint cycle by the time this is called
     * (it runs after {@code moduleViewHandler.paintComponent} above), so every component's connector points are
     * safe to read.
     */
    private void paintJmsDestinationConnectors(Graphics graphics, Module ikasanModule) {
        if (ikasanModule == null || !(graphics instanceof Graphics2D) || !IkasanStudioSettings.areJmsConnectorsEnabled()) {
            return;
        }
        List<JmsFlowConnections.JmsLink> links = JmsFlowConnections.findMatchingLinks(ikasanModule);
        if (links.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setColor(JMS_CONNECTOR_COLOR);
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, JMS_CONNECTOR_DASH, 0));
            int index = 0;
            for (JmsFlowConnections.JmsLink link : links) {
                IkasanFlowViewHandler producerFlowHandler = flowHandlerFor(link.producer());
                IkasanFlowViewHandler consumerFlowHandler = flowHandlerFor(link.consumer());
                AbstractViewHandlerIntellij producerHandler = producerFlowHandler != null ? producerFlowHandler.getEndpointViewHandlerFor(link.producer()) : null;
                AbstractViewHandlerIntellij consumerHandler = consumerFlowHandler != null ? consumerFlowHandler.getEndpointViewHandlerFor(link.consumer()) : null;
                if (producerFlowHandler == null || consumerFlowHandler == null || producerHandler == null || consumerHandler == null) {
                    continue;
                }
                paintElbowConnector(g2d, producerHandler.getRightConnectorPoint(), consumerHandler.getLeftConnectorPoint(),
                        producerFlowHandler, consumerFlowHandler, index);
                index++;
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * A consumer/producer's own in-route box is not what's visually at the edge of its flow - Consumer and
     * Producer components are also drawn with a separate, externally-positioned "channel endpoint" pill
     * outside the flow's border (see {@code IkasanFlowRouteViewHandler#displayExternalEndpointIfExists}), and
     * that pill is what a connector line needs to touch. This gets the containing flow's own handler, needed
     * both to resolve the pill ({@link IkasanFlowViewHandler#getEndpointViewHandlerFor}) and to route around
     * that flow's own content (see {@link #paintElbowConnector}).
     */
    private IkasanFlowViewHandler flowHandlerFor(FlowElement owner) {
        Flow containingFlow = owner != null ? owner.getContainingFlow() : null;
        return containingFlow != null ? ViewHandlerCache.getFlowViewHandler(project, containingFlow) : null;
    }

    /**
     * Routes through the empty gap between the two flows' rows, never through either flow's own components:
     * down from the producer's pill into that gap, left along the gap to a corridor left of every flow's
     * shared left edge (flows are always left-aligned to the same X, see {@code IkasanModuleViewHandler}), down
     * that corridor to the consumer's own row, then right into its pill - matching the "down, left, down,
     * right" shape a straight point-to-point or single-side-gutter line can't achieve without cutting across
     * whichever flow it passes over. Each additional link (index &gt; 0) is staggered slightly so parallel
     * links don't draw directly on top of one another.
     * -
     * The horizontal corridor hugs the BOTTOM of the gap (just above the lower flow's own top edge), not the
     * midpoint: an incomplete upper flow's own "getting started" hint text (see {@code paintGettingStartedHint})
     * is drawn with no background fill, growing downward starting right at that flow's own bottom edge - it's
     * exactly why {@code gapAfterFlow} widens the gap to fit it in the first place - so a line sitting in the
     * gap's own upper portion would visibly run through the hint's bare glyphs. Hints only ever grow downward
     * from the upper flow, never upward from the lower one, so hugging the bottom clears them reliably.
     * -
     * Known limitation: if another flow's row lies between the producer's and the consumer's (not adjacent),
     * the horizontal corridor may still cross that intervening row - out of scope for this pass.
     */
    private void paintElbowConnector(Graphics2D g2d, Point start, Point end,
                                     IkasanFlowViewHandler producerFlowHandler, IkasanFlowViewHandler consumerFlowHandler, int index) {
        IkasanFlowViewHandler upperFlow = producerFlowHandler.getTopY() <= consumerFlowHandler.getTopY() ? producerFlowHandler : consumerFlowHandler;
        IkasanFlowViewHandler lowerFlow = upperFlow == producerFlowHandler ? consumerFlowHandler : producerFlowHandler;
        int corridorY = Math.max(upperFlow.getBottomY() + JMS_CONNECTOR_GAP_CLEARANCE,
                lowerFlow.getTopY() - JMS_CONNECTOR_GAP_CLEARANCE - (index * JMS_CONNECTOR_STAGGER));
        // Derived from the pill's own entry point (end.x), not consumerFlowHandler.getLeftX() - the pill is
        // drawn OUTSIDE its flow's own left border (see displayExternalEndpointIfExists), so the flow box's
        // edge alone sits well short of clearing the pill itself.
        int approachX = end.x - JMS_CONNECTOR_GUTTER_MARGIN - (index * JMS_CONNECTOR_STAGGER);
        // A small clear-of-the-pill stand-off right after exiting, before turning down into the gap - turning
        // immediately at start.x draws the corner right on the pill's own edge.
        int standoffX = start.x + JMS_CONNECTOR_STANDOFF;

        Path2D path = new Path2D.Double();
        path.moveTo(start.x, start.y);
        path.lineTo(standoffX, start.y);
        path.lineTo(standoffX, corridorY);
        path.lineTo(approachX, corridorY);
        path.lineTo(approachX, end.y);
        path.lineTo(end.x, end.y);
        g2d.draw(path);
        paintArrowhead(g2d, end);
    }

    /** A small filled triangle pointing right, into the consumer's left edge - fill isn't affected by the
     * connector's dashed stroke, so no stroke needs saving/restoring around this. */
    private void paintArrowhead(Graphics2D g2d, Point tip) {
        Polygon arrow = new Polygon();
        arrow.addPoint(tip.x, tip.y);
        arrow.addPoint(tip.x - JMS_CONNECTOR_ARROW_SIZE, tip.y - (JMS_CONNECTOR_ARROW_SIZE / 2));
        arrow.addPoint(tip.x - JMS_CONNECTOR_ARROW_SIZE, tip.y + (JMS_CONNECTOR_ARROW_SIZE / 2));
        g2d.fill(arrow);
    }

    // Gap between the anchor's own real Email Endpoint pill and this node, matching the rhythm of
    // JMS_CONNECTOR_GUTTER_MARGIN rather than inventing a new spacing constant.
    private static final int TEST_MAIL_SERVER_NODE_GAP = JMS_CONNECTOR_GUTTER_MARGIN;
    // Matches mailserver.svg/mailserver_dark.svg's own canvas size (same convention as every other endpoint
    // icon, e.g. Email Endpoint's normal.svg).
    private static final int TEST_MAIL_SERVER_NODE_WIDTH = 90;
    private static final int TEST_MAIL_SERVER_NODE_HEIGHT = 60;
    private static final int TEST_MAIL_SERVER_LABEL_GAP = 4;
    private static final String TEST_MAIL_SERVER_LABEL = "Test Mail Server";

    /**
     * Draws the shared "Test Mail Server" node immediately to the right of the first flow (in module order)
     * that has a matching Email Producer - positioned exactly like that producer's own real Email Endpoint
     * pill would be, one gap further right - with a connector line from every OTHER matching Email Producer
     * elsewhere in the module (which, being later in module order, is always positioned lower on the canvas -
     * see {@code IkasanModuleViewHandler}) routed upward into it. Only for a Link whose address a
     * locally-launched test mail server (MailHog) is actually listening on right now - see
     * {@link TestMailServerLinks#findLinks} (grouping, framework-independent) and
     * {@link TestMailServerSessionService#isListening} (the live, polled state); nothing is drawn at all
     * otherwise, so the node/lines simply vanish the poll tick after the user stops it (see
     * {@code StopTestMailServerAction}) - there is deliberately no separate "was it ever started" state to go
     * stale.
     * -
     * Module-level, like {@link #paintJmsDestinationConnectors} above and for the same reason: the matching
     * Email Producers can be spread across several different flows.
     */
    private void paintTestMailServerNode(Graphics graphics, Module ikasanModule) {
        if (ikasanModule == null || ikasanModule.getFlows() == null || !(graphics instanceof Graphics2D)) {
            return;
        }
        List<TestMailServerLinks.Link> links = TestMailServerLinks.findLinks(ikasanModule);
        if (links.isEmpty()) {
            return;
        }
        TestMailServerSessionService sessionService = project.getService(TestMailServerSessionService.class);

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            for (TestMailServerLinks.Link link : links) {
                if (sessionService.isListening(link.host(), link.port())) {
                    paintTestMailServerLink(graphics, g2d, ikasanModule, link);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void paintTestMailServerLink(Graphics graphics, Graphics2D g2d, Module ikasanModule, TestMailServerLinks.Link link) {
        FlowElement anchorProducer = firstByModuleOrder(ikasanModule, link.producers());
        IkasanFlowViewHandler anchorFlowHandler = flowHandlerFor(anchorProducer);
        IkasanFlowComponentViewHandler anchorEndpointHandler = anchorFlowHandler != null ? anchorFlowHandler.getEndpointViewHandlerFor(anchorProducer) : null;
        if (anchorEndpointHandler == null) {
            return;
        }

        Point anchorRight = anchorEndpointHandler.getRightConnectorPoint();
        int nodeLeftX = anchorRight.x + TEST_MAIL_SERVER_NODE_GAP;
        int nodeTopY = anchorRight.y - (TEST_MAIL_SERVER_NODE_HEIGHT / 2);
        Point nodeLeft = new Point(nodeLeftX, anchorRight.y);

        // Deliberately no g2d.setColor() call here, same as drawConnector() above (the plain lines joining
        // ordinary flow components) - both render in whatever the inherited default paint colour is, which is
        // exactly how they end up matching without this needing to guess at and hard-code that colour itself.
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(anchorRight.x, anchorRight.y, nodeLeft.x, nodeLeft.y);
        paintArrowhead(g2d, nodeLeft);

        // Every other matching producer is, by definition of "anchor = first in module order", in a flow
        // positioned lower on the canvas. Rather than the JMS connectors' own per-pair gap-routing (which,
        // sharing the canvas with an existing JMS line, could end up visually overlapping it - see the anchor
        // line above for why this feature draws its own dedicated line instead), every other producer instead
        // branches into one shared vertical trunk hanging straight down from the midpoint of the anchor's own
        // line - simpler, and reads as one visibly distinct spine of "things pointing at this Test Mail
        // Server", however many flows are on it.
        int dropX = (anchorRight.x + nodeLeft.x) / 2;
        for (FlowElement producer : link.producers()) {
            if (producer == anchorProducer) {
                continue;
            }
            IkasanFlowViewHandler otherFlowHandler = flowHandlerFor(producer);
            IkasanFlowComponentViewHandler otherEndpointHandler = otherFlowHandler != null ? otherFlowHandler.getEndpointViewHandlerFor(producer) : null;
            if (otherEndpointHandler == null) {
                continue;
            }
            Point otherRight = otherEndpointHandler.getRightConnectorPoint();
            Path2D path = new Path2D.Double();
            path.moveTo(dropX, anchorRight.y);
            path.lineTo(dropX, otherRight.y);
            path.lineTo(otherRight.x, otherRight.y);
            g2d.draw(path);
        }

        paintTestMailServerIcon(graphics, nodeLeftX, nodeTopY);
    }

    /** @return the element of {@code candidates} belonging to the flow that comes first in {@code module.getFlows()}. */
    private FlowElement firstByModuleOrder(Module module, List<FlowElement> candidates) {
        for (Flow flow : module.getFlows()) {
            for (FlowElement candidate : candidates) {
                if (candidate.getContainingFlow() == flow) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void paintTestMailServerIcon(Graphics graphics, int nodeLeftX, int nodeTopY) {
        Icon icon = IkasanComponentLibrary.getMailServerIcon();
        icon.paintIcon(this, graphics, nodeLeftX, nodeTopY);
        StudioUIUtils.drawCenteredStringFromTopCentre(graphics, PaintMode.PAINT, TEST_MAIL_SERVER_LABEL,
                nodeLeftX + (TEST_MAIL_SERVER_NODE_WIDTH / 2), nodeTopY + TEST_MAIL_SERVER_NODE_HEIGHT + TEST_MAIL_SERVER_LABEL_GAP,
                TEST_MAIL_SERVER_NODE_WIDTH + 60, StudioUIUtils.getMainFont());
    }

    private static final int FLOW_ERROR_FLASH_INTERVAL_MS = 500;

    /**
     * Drives {@link #flowErrorFlashOn} - the 500ms on/off tick consumed by {@code paintFlowTransportControls}'
     * {@code statusLabelColor} to flash the "Stopped in Error" status text red. Used to also draw a matching red
     * outline around the whole flow, but that competed with the status text for attention rather than
     * reinforcing it - the status text is the one thing worth hovering over (it reveals the error detail), so
     * it's now the sole flashing "look here" cue; a flow's error state otherwise only shows via its (static,
     * non-flashing) transport-control status label. The timer still stops itself the poll tick after every
     * flagged flow recovers, same "no separate state to go stale" approach as {@link #paintTestMailServerNode}.
     * -
     * (Re)started/stopped here, on every paint, rather than by the monitor service itself: this keeps the
     * service free of any UI/Swing-timer concerns (it only ever calls {@code canvas.repaint()}), and naturally
     * self-corrects if the canvas is ever repainted for an unrelated reason while the flag is stale.
     */
    private void updateFlowErrorFlashState(Module ikasanModule) {
        if (ikasanModule == null || ikasanModule.getFlows() == null) {
            return;
        }
        FlowErrorStates errorStates = project.getService(FlowErrorMonitorService.class).getErrorStates();
        updateFlowErrorFlashTimer(errorStates.hasAnyFlagged());
    }

    private void updateFlowErrorFlashTimer(boolean anyFlagged) {
        if (anyFlagged) {
            if (flowErrorFlashTimer == null) {
                flowErrorFlashTimer = new Timer(FLOW_ERROR_FLASH_INTERVAL_MS, e -> {
                    flowErrorFlashOn = !flowErrorFlashOn;
                    repaint();
                });
                flowErrorFlashTimer.start();
            }
        } else if (flowErrorFlashTimer != null) {
            flowErrorFlashTimer.stop();
            flowErrorFlashTimer = null;
            flowErrorFlashOn = false;
        }
    }

    private static final int TRANSPORT_BUTTON_SIZE = 14;
    private static final int TRANSPORT_BUTTON_GAP = 3;
    // Fixed left-hand gutter for every flow's transport-control buttons - deliberately NOT computed relative to
    // each flow's own left edge (the original approach): that left the buttons crowding the flow's leftmost
    // endpoint icon instead of reading as a distinct control column. Pinning it here, flush near the canvas's own
    // left edge, keeps the whole column visually separate from the flows regardless of how far right
    // IkasanModuleViewHandler.FLOW_X_START_POINT positions them.
    private static final int TRANSPORT_BUTTONS_LEFT_X = 24;
    private static final int TRANSPORT_STATUS_LABEL_GAP = 6;
    // Deliberately just these 4 - the same set (and order) exposed by IntelliJ's own Run/Debug console for a
    // flow: Start, Pause, Stop, Start-Paused. There's a 5th Ikasan action (plain "resume" from Paused back to
    // Running) that the console doesn't surface as its own button either - see FlowTransportAction#isEnabledFor's
    // javadoc for how Start doubles as "resume" here.
    private static final FlowTransportAction[] TRANSPORT_BUTTON_ORDER = {
            FlowTransportAction.START, FlowTransportAction.PAUSE, FlowTransportAction.STOP, FlowTransportAction.START_PAUSE
    };
    private static final int TRANSPORT_BUTTON_STACK_HEIGHT =
            (TRANSPORT_BUTTON_ORDER.length * TRANSPORT_BUTTON_SIZE) + ((TRANSPORT_BUTTON_ORDER.length - 1) * TRANSPORT_BUTTON_GAP);

    /**
     * Where the button stack (and status label) should start vertically for a given flow - centred on the flow's
     * own vertical midpoint (its title bar included), not pinned to its top edge, so the controls read as
     * belonging to the flow as a whole rather than sitting up near its title. Shared by the paint method and
     * both of its hit-test twins below so the 3 never drift out of sync with each other.
     */
    private int transportButtonsTopY(IkasanFlowViewHandler flowHandler) {
        return flowHandler.getCentrePoint().y - (TRANSPORT_BUTTON_STACK_HEIGHT / 2);
    }

    /**
     * Draws the 4 transport-control buttons (Start/Pause/Stop/Start-Paused) and a status label
     * (Running/Stopped/Paused/Stopped in Error/Unknown) to the left of every flow, sourced from
     * {@link FlowErrorMonitorService#getFlowStatuses()} - the same poll loop that drives
     * {@link #updateFlowErrorFlashState}, just reading the raw per-flow state it already tracks rather than only
     * the error flag. Lets a flow be started/stopped/paused, and its status checked, without ever opening IntelliJ's
     * Run/Debug console. Click handling lives in {@link #getFlowTransportButtonAtXY}, hover tooltips in
     * {@link #mouseMoveAction}.
     */
    private void paintFlowTransportControls(Graphics graphics, Module ikasanModule) {
        if (ikasanModule == null || ikasanModule.getFlows() == null || !(graphics instanceof Graphics2D)
                || !project.getService(IkasanDebugSessionService.class).isModuleRunning()) {
            return;
        }
        FlowRuntimeStatuses flowStatuses = project.getService(FlowErrorMonitorService.class).getFlowStatuses();
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            for (Flow flow : ikasanModule.getFlows()) {
                if (flow == null) {
                    continue;
                }
                IkasanFlowViewHandler flowHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
                if (flowHandler == null) {
                    continue;
                }
                String rawState = flowStatuses.getRawState(flow.getIdentity());
                if (rawState == null) {
                    // Nothing to show while the module isn't running (or hasn't been reached by a poll yet) -
                    // the buttons/status only have relevance once there's a real state to act on or display; a
                    // permanently-disabled "Unknown" row for every flow before the module is even started was
                    // just noise.
                    continue;
                }
                int buttonLeftX = TRANSPORT_BUTTONS_LEFT_X;
                int buttonsTopY = transportButtonsTopY(flowHandler);

                for (int i = 0; i < TRANSPORT_BUTTON_ORDER.length; i++) {
                    FlowTransportAction action = TRANSPORT_BUTTON_ORDER[i];
                    int buttonTopY = buttonsTopY + i * (TRANSPORT_BUTTON_SIZE + TRANSPORT_BUTTON_GAP);
                    boolean enabled = FlowTransportAction.isEnabledFor(action, rawState);
                    Color buttonColor = enabled ? action.getColor() : ThemeAwareColors.getDisabledTextColor();
                    paintTransportButtonShape(g2d, action, buttonLeftX, buttonTopY, buttonColor);
                }

                String statusLabel = flowStatusDisplayLabel(rawState);
                Font statusFont = StudioUIUtils.getMainFont();
                int labelLeftX = buttonLeftX + TRANSPORT_BUTTON_SIZE + TRANSPORT_STATUS_LABEL_GAP;
                // One word per line - trades the vertical room the button stack already has (4 rows tall) for
                // horizontal room, so "Stopped in Error" wraps to 3 short lines instead of one wide one that
                // either pushes the flow further right or gets clipped/overlapped. Single-word statuses
                // (Running/Stopped/Paused/Unknown/Recovering) just render as their usual single line.
                String[] statusWords = statusLabel.split(" ");
                int lineHeight = StudioUIUtils.getTextHeight(g2d, statusFont);
                int textBlockHeight = statusWords.length * lineHeight;
                int labelTopY = buttonsTopY + (TRANSPORT_BUTTON_STACK_HEIGHT / 2) - (textBlockHeight / 2);
                Color originalColor = g2d.getColor();
                g2d.setColor(statusLabelColor(statusLabel));
                for (int i = 0; i < statusWords.length; i++) {
                    StudioUIUtils.drawStringLeftAlignedFromTopLeft(g2d, statusWords[i], labelLeftX, labelTopY + (i * lineHeight), statusFont);
                }
                g2d.setColor(originalColor);
            }
        } finally {
            g2d.dispose();
        }
    }

    // All 4 shapes are authored on a 16x16 grid (matching the retired flow-*.svg icons they replace) and scaled
    // to TRANSPORT_BUTTON_SIZE below - drawn directly, rather than as static icon files, so the colour can vary
    // per action and per enabled/disabled state (see FlowTransportAction#getColor) without needing a coloured
    // variant of every icon for every state.
    private static final double TRANSPORT_ICON_GRID = 16.0;

    /** Draws one transport-control button's glyph at (x,y), filled with {@code color} - see {@link #paintFlowTransportControls}. */
    private void paintTransportButtonShape(Graphics2D g2d, FlowTransportAction action, int x, int y, Color color) {
        AffineTransform originalTransform = g2d.getTransform();
        Color originalColor = g2d.getColor();
        Object originalAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.translate(x, y);
            g2d.scale(TRANSPORT_BUTTON_SIZE / TRANSPORT_ICON_GRID, TRANSPORT_BUTTON_SIZE / TRANSPORT_ICON_GRID);
            g2d.setColor(color);
            switch (action) {
                case START -> g2d.fill(triangle(4, 2.5, 13, 8, 4, 13.5));
                case STOP -> g2d.fill(new RoundRectangle2D.Double(3, 3, 10, 10, 2, 2));
                case PAUSE -> {
                    g2d.fill(new RoundRectangle2D.Double(3.5, 2.5, 3, 11, 1.5, 1.5));
                    g2d.fill(new RoundRectangle2D.Double(9.5, 2.5, 3, 11, 1.5, 1.5));
                }
                case START_PAUSE -> {
                    g2d.fill(triangle(1.5, 2.5, 8.5, 8, 1.5, 13.5));
                    g2d.fill(new RoundRectangle2D.Double(10.5, 2.5, 2, 11, 1.2, 1.2));
                    g2d.fill(new RoundRectangle2D.Double(13.5, 2.5, 2, 11, 1.2, 1.2));
                }
            }
        } finally {
            g2d.setTransform(originalTransform);
            g2d.setColor(originalColor);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    originalAntialiasing != null ? originalAntialiasing : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        }
    }

    private static Path2D.Double triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();
        return path;
    }

    private static final String UNKNOWN_DISPLAY_LABEL = "Unknown";
    private static final String STOPPED_DISPLAY_LABEL = "Stopped";
    private static final String STOPPED_IN_ERROR_DISPLAY_LABEL = "Stopped in Error";
    private static final String RUNNING_DISPLAY_LABEL = "Running";
    private static final String PAUSED_DISPLAY_LABEL = "Paused";

    /** @return the human-readable status label for a flow's raw Ikasan state - see org.ikasan.spec.flow.Flow's state constants. */
    private String flowStatusDisplayLabel(String rawState) {
        if (rawState == null) {
            return UNKNOWN_DISPLAY_LABEL;
        }
        return switch (rawState) {
            case "running" -> RUNNING_DISPLAY_LABEL;
            case "stopped" -> STOPPED_DISPLAY_LABEL;
            case "paused" -> PAUSED_DISPLAY_LABEL;
            case "stoppedInError" -> STOPPED_IN_ERROR_DISPLAY_LABEL;
            case "recovering" -> "Recovering";
            default -> rawState;
        };
    }

    /**
     * The status label's text colour, per the user's own spec for this feature: grey while Unknown, green while
     * Running, orange while Paused, red while Stopped, and red flashing while Stopped in Error - the flash in
     * lockstep with {@link #updateFlowErrorFlashState}'s own {@link #flowErrorFlashOn} tick (that method always
     * runs earlier in the same paint pass - see the call order in {@code paint()} - so its flash state is already
     * current by the time this reads it). "Recovering" isn't part of the spec - falls back to the default text
     * colour, same as any state that reaches here unrecognised.
     */
    private Color statusLabelColor(String statusLabel) {
        if (UNKNOWN_DISPLAY_LABEL.equals(statusLabel)) {
            return ThemeAwareColors.getDisabledTextColor();
        }
        if (RUNNING_DISPLAY_LABEL.equals(statusLabel)) {
            return ThemeAwareColors.getSuccessColor();
        }
        if (PAUSED_DISPLAY_LABEL.equals(statusLabel)) {
            return FlowTransportAction.pauseOrange();
        }
        if (STOPPED_DISPLAY_LABEL.equals(statusLabel)) {
            return FlowTransportAction.stopRed();
        }
        if (STOPPED_IN_ERROR_DISPLAY_LABEL.equals(statusLabel)) {
            return flowErrorFlashOn ? FlowTransportAction.stopRed() : ThemeAwareColors.getTextColor();
        }
        return ThemeAwareColors.getTextColor();
    }

    /**
     * Given x,y coords, check whether the pointer is over one of a flow's transport-control buttons (see
     * {@link #paintFlowTransportControls}) - purely geometric, regardless of whether that button is currently
     * enabled, so a disabled (dimmed) button still shows its own tooltip in {@link #mouseMoveAction}.
     * {@link #mouseClickAction} additionally checks {@link FlowTransportAction#isEnabledFor} before firing one.
     */
    private Pair<Flow, FlowTransportAction> getFlowTransportButtonAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        if (ikasanModule == null || ikasanModule.getFlows() == null
                || !project.getService(IkasanDebugSessionService.class).isModuleRunning()) {
            return null;
        }
        FlowRuntimeStatuses flowStatuses = project.getService(FlowErrorMonitorService.class).getFlowStatuses();
        for (Flow flow : ikasanModule.getFlows()) {
            if (flow == null || flowStatuses.getRawState(flow.getIdentity()) == null) {
                continue;
            }
            IkasanFlowViewHandler flowHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            if (flowHandler == null) {
                continue;
            }
            if (xpos < TRANSPORT_BUTTONS_LEFT_X || xpos > TRANSPORT_BUTTONS_LEFT_X + TRANSPORT_BUTTON_SIZE) {
                continue;
            }
            int buttonsTopY = transportButtonsTopY(flowHandler);
            for (int i = 0; i < TRANSPORT_BUTTON_ORDER.length; i++) {
                int buttonTopY = buttonsTopY + i * (TRANSPORT_BUTTON_SIZE + TRANSPORT_BUTTON_GAP);
                if (ypos >= buttonTopY && ypos <= buttonTopY + TRANSPORT_BUTTON_SIZE) {
                    return new Pair<>(flow, TRANSPORT_BUTTON_ORDER[i]);
                }
            }
        }
        return null;
    }

    /**
     * Given x,y coords, check whether the pointer is over a flow's status label (see
     * {@link #paintFlowTransportControls}) - used only to decide whether {@link #mouseMoveAction} should show the
     * flow's latest error as a tooltip, so a hit here doesn't need to distinguish which status is showing.
     */
    private Flow getFlowStatusLabelOwnerAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        if (ikasanModule == null || ikasanModule.getFlows() == null
                || !project.getService(IkasanDebugSessionService.class).isModuleRunning()) {
            return null;
        }
        FlowRuntimeStatuses flowStatuses = project.getService(FlowErrorMonitorService.class).getFlowStatuses();
        for (Flow flow : ikasanModule.getFlows()) {
            if (flow == null || flowStatuses.getRawState(flow.getIdentity()) == null) {
                continue;
            }
            IkasanFlowViewHandler flowHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
            if (flowHandler == null) {
                continue;
            }
            int labelLeftX = TRANSPORT_BUTTONS_LEFT_X + TRANSPORT_BUTTON_SIZE + TRANSPORT_STATUS_LABEL_GAP;
            int buttonsTopY = transportButtonsTopY(flowHandler);
            int labelBottomY = buttonsTopY + TRANSPORT_BUTTON_STACK_HEIGHT;
            int labelTopY = buttonsTopY;
            // Generous, un-measured horizontal band to the right of the button column - the label's actual
            // width varies per status text and isn't worth recomputing here just to shrink a hover target.
            if (xpos > labelLeftX && xpos < labelLeftX + 160 && ypos >= labelTopY && ypos <= labelBottomY) {
                return flow;
            }
        }
        return null;
    }

    /**
     * The Run module button should only be enabled once the canvas contains at least one complete,
     * valid flow (i.e. we are past the point of hinting what needs adding to the design).
     */
    private void updateRunModuleButtonState(Module module) {
        GettingStartedHint hint = getGettingStartedHint(module);
        boolean validModule = hint == GettingStartedHint.READY_TO_RUN || hint == GettingStartedHint.OPEN_CONSOLE;
        boolean runnable = validModule
                && !project.getService(IkasanDebugSessionService.class).isModuleRunning();
        CanvasPanel canvasPanel = project.getService(UiContext.class).getCanvasPanel();
        if (canvasPanel != null) {
            canvasPanel.setRunModuleEnabled(runnable);
            canvasPanel.setDebugModuleEnabled(runnable);
            canvasPanel.setStopModuleEnabled(
                    project.getService(IkasanDebugSessionService.class).canStopModule());
        }
    }

    public enum GettingStartedHint {
        NONE,
        NO_FLOWS,
        EMPTY_FLOW,
        ADD_COMPONENTS,
        READY_TO_RUN,
        OPEN_CONSOLE
    }

    static GettingStartedHint getGettingStartedHint(Module module) {
        if (module == null || !module.isInitialised()) {
            return GettingStartedHint.NONE;
        }
        if (module.getFlows() == null || module.getFlows().isEmpty()) {
            return GettingStartedHint.NO_FLOWS;
        }
        boolean flowNeedsConsumer = module.getFlows().stream()
                .anyMatch(flow -> flow != null && !flow.hasConsumer());
        if (flowNeedsConsumer) {
            return GettingStartedHint.EMPTY_FLOW;
        }
        boolean flowIsIncomplete = module.getFlows().stream()
                .anyMatch(flow -> flow != null && !flow.getFlowIntegrityStatus().isBlank());
        return flowIsIncomplete ? GettingStartedHint.ADD_COMPONENTS : GettingStartedHint.READY_TO_RUN;
    }

    private void paintGettingStartedHint(Graphics graphics, Module module) {
        GettingStartedHint moduleHint = getGettingStartedHint(module);
        if (moduleHint == GettingStartedHint.READY_TO_RUN) {
            // Once the flow is valid there's always something to say - either "you can keep adding
            // components" (READY_TO_RUN, despite the name) or, briefly after launch, "open the console".
            // Unlike the other onboarding hints this one is never fully suppressed.
            PropertiesComponent properties = PropertiesComponent.getInstance(project);
            if (properties.getBoolean(MODULE_LAUNCHED_PROPERTY, false) && !properties.getBoolean(CONSOLE_OPENED_PROPERTY, false)) {
                moduleHint = GettingStartedHint.OPEN_CONSOLE;
            }
        }
        if (moduleHint == GettingStartedHint.NONE) {
            return;
        }

        boolean detailed = IkasanStudioSettings.areGettingStartedHintsEnabled();
        if (moduleHint != GettingStartedHint.NO_FLOWS && !detailed) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (moduleHint == GettingStartedHint.NO_FLOWS) {
                drawHintBlock(g, moduleHint, getWidth() / 2, getHeight() / 2, detailed);
            } else if (moduleHint == GettingStartedHint.EMPTY_FLOW || moduleHint == GettingStartedHint.ADD_COMPONENTS) {
                // Each incomplete flow gets its own hint, positioned under itself, judged independently of
                // its siblings - previously a single global hint category was picked for the whole module and
                // anchored under just the first flow matching it, so with two flows both needing attention
                // (even for different reasons) only one of them ever showed a visible hint at all.
                for (Flow flow : module.getFlows()) {
                    GettingStartedHint flowHint = getFlowHint(flow);
                    if (flowHint == null) {
                        continue;
                    }
                    IkasanFlowViewHandler handler = ViewHandlerCache.getFlowViewHandler(project, flow);
                    if (handler == null) {
                        continue;
                    }
                    int centreX = (handler.getLeftX() + handler.getRightX()) / 2;
                    int centreY = handler.getBottomY() + JBUI.scale(28);
                    drawHintBlock(g, flowHint, centreX, centreY, detailed);
                }
            } else {
                // READY_TO_RUN / OPEN_CONSOLE - a whole-module status once every flow is already complete, so
                // (unlike EMPTY_FLOW/ADD_COMPONENTS above) there's nothing per-flow to say - anchor below
                // whichever flow sits lowest on the canvas (largest bottomY), not just the first one in list
                // order. Anchoring under an arbitrary "first" flow could land the hint in the gap immediately
                // above a second flow stacked closely below it, overlapping that flow's own label/box - picking
                // the lowest flow guarantees nothing else is rendered below the hint to collide with.
                IkasanFlowViewHandler lowestHandler = null;
                for (Flow flow : module.getFlows()) {
                    if (flow == null) {
                        continue;
                    }
                    IkasanFlowViewHandler handler = ViewHandlerCache.getFlowViewHandler(project, flow);
                    if (handler != null && (lowestHandler == null || handler.getBottomY() > lowestHandler.getBottomY())) {
                        lowestHandler = handler;
                    }
                }
                int centreX = getWidth() / 2;
                int centreY = getHeight() / 2;
                if (lowestHandler != null) {
                    centreX = (lowestHandler.getLeftX() + lowestHandler.getRightX()) / 2;
                    centreY = lowestHandler.getBottomY() + JBUI.scale(28);
                }
                drawHintBlock(g, moduleHint, centreX, centreY, detailed);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * @return this flow's own getting-started hint (EMPTY_FLOW if it has no consumer, ADD_COMPONENTS if it has
     * a consumer but is otherwise incomplete), or null if the flow is already complete. Lets each flow be
     * judged independently, rather than one incomplete flow's category winning for the whole module (see
     * getGettingStartedHint) and masking a sibling flow's own, possibly different, issue.
     */
    public static GettingStartedHint getFlowHint(Flow flow) {
        if (flow == null) {
            return null;
        }
        if (!flow.hasConsumer()) {
            return GettingStartedHint.EMPTY_FLOW;
        }
        if (!flow.getFlowIntegrityStatus().isBlank()) {
            return GettingStartedHint.ADD_COMPONENTS;
        }
        return null;
    }

    private static String headingTextFor(GettingStartedHint hint) {
        return switch (hint) {
            case NO_FLOWS -> StudioBundle.message("label.NoFlowsAdded");
            case EMPTY_FLOW -> StudioBundle.message("label.AddAConsumer");
            case ADD_COMPONENTS -> StudioBundle.message("label.AddAProducer");
            case READY_TO_RUN -> StudioBundle.message("label.AddAComponent");
            case OPEN_CONSOLE -> StudioBundle.message("label.ModuleStarting");
            default -> "";
        };
    }

    private static String instructionTextFor(GettingStartedHint hint) {
        return switch (hint) {
            case NO_FLOWS -> StudioBundle.message("message.DragAFlowFromThePaletteOntoTheCanvas");
            case EMPTY_FLOW -> StudioBundle.message("message.DragAConsumerFromThePaletteOntoThisFlow");
            case ADD_COMPONENTS -> StudioBundle.message("message.DragAProducerFromThePaletteOntoThisFlow");
            case READY_TO_RUN -> StudioBundle.message("message.SelectRunModuleOrUseIntellijRunOrDebug");
            case OPEN_CONSOLE -> StudioBundle.message("message.WaitForModuleStartupToComplete");
            default -> "";
        };
    }

    // Text is centred on centreX, so a long single line can run off the left edge of whatever's currently
    // scrolled into view. Wrap onto extra lines instead. Deriving this from the live viewport extent didn't
    // pan out in practice, so a fixed width is used for now. Shared by drawHintBlock and measureHintBlockHeight
    // so a hint's measured height always matches what actually gets drawn. Scaled per-call (not cached), same
    // as every other JBUI.scale(...) call here, so a runtime DPI/scaling change is still picked up live.
    private static int hintMaxTextWidth() {
        return JBUI.scale(420);
    }

    /**
     * Draws one heading (+ instruction, when detailed) block of getting-started hint text centred on centreX,
     * with its heading's top edge at centreY - callers may invoke this multiple times per paint (once per
     * incomplete flow) to show several hints at once, each positioned under the flow it applies to.
     */
    private void drawHintBlock(Graphics2D g, GettingStartedHint hint, int centreX, int centreY, boolean detailed) {
        String heading = headingTextFor(hint);
        String instruction = instructionTextFor(hint);

        // A flow with a consumer but no producer will not run, so make this hint stand out rather than
        // blending in with the other, purely informational, getting-started hints.
        boolean needsProducer = hint == GettingStartedHint.ADD_COMPONENTS;
        // Label.disabledForeground previously used for the instruction line is designed for disabled controls,
        // not body text painted straight onto the canvas background - too low-contrast to read in either theme.
        // The heading's bold weight already gives it enough visual priority over the (regular-weight)
        // instruction line, so both can safely share the same readable, theme-aware text color.
        Color textColor = ThemeAwareColors.getTextColor();
        Color headingColor = needsProducer ? ThemeAwareColors.getWarningColor() : textColor;
        Color instructionColor = needsProducer ? ThemeAwareColors.getWarningColor() : textColor;
        Font headingFont = getFont().deriveFont(Font.BOLD, getFont().getSize2D() + 2f);
        Font instructionFont = getFont();

        g.setColor(headingColor);
        g.setFont(headingFont);
        FontMetrics headingMetrics = g.getFontMetrics(headingFont);
        List<String> headingLines = wrapText(heading, headingMetrics, hintMaxTextWidth());
        int headingLineHeight = headingMetrics.getHeight();
        int headingBaseline = centreY - JBUI.scale(5);
        for (String line : headingLines) {
            g.drawString(line, centreX - headingMetrics.stringWidth(line) / 2, headingBaseline);
            headingBaseline += headingLineHeight;
        }

        if (detailed) {
            g.setColor(instructionColor);
            g.setFont(instructionFont);
            FontMetrics instructionMetrics = g.getFontMetrics(instructionFont);
            List<String> instructionLines = wrapText(instruction, instructionMetrics, hintMaxTextWidth());
            int instructionLineHeight = instructionMetrics.getHeight();
            // headingBaseline has already been advanced past the last heading line drawn above, so this
            // preserves the original ~22px gap after the (possibly multi-line) heading block.
            int instructionBaseline = headingBaseline - headingLineHeight + JBUI.scale(22);
            for (String line : instructionLines) {
                g.drawString(line, centreX - instructionMetrics.stringWidth(line) / 2, instructionBaseline);
                instructionBaseline += instructionLineHeight;
            }
        }
    }

    /**
     * The total vertical space (in px, below a flow's own getBottomY()) that drawHintBlock will actually use to
     * render this hint at that same position - i.e. how far centreY (== bottomY + 28, see
     * paintGettingStartedHint) needs to be from the NEXT flow's own top edge to avoid the two overlapping.
     * Mirrors drawHintBlock's geometry exactly (same fonts/wrapping/gaps) rather than an approximation, so
     * layout (IkasanModuleViewHandler, which reserves this much space) and painting never drift out of sync.
     * @param graphics used only for FontMetrics - never drawn to.
     * @param baseFont the canvas's own (non-bold, non-derived) font - matches what drawHintBlock derives from
     *                  getFont() when actually painting.
     */
    public static int measureHintBlockHeight(Graphics graphics, Font baseFont, GettingStartedHint hint, boolean detailed) {
        if (hint == null || hint == GettingStartedHint.NONE) {
            return 0;
        }
        Font headingFont = baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 2f);
        FontMetrics headingMetrics = graphics.getFontMetrics(headingFont);
        List<String> headingLines = wrapText(headingTextFor(hint), headingMetrics, hintMaxTextWidth());
        int headingLineHeight = headingMetrics.getHeight();

        // Offset of the last drawn line's baseline from bottomY, replicating drawHintBlock's own maths:
        // headingBaseline starts at (bottomY + 28) - 5, then advances by headingLineHeight per heading line.
        int offset = JBUI.scale(28) - JBUI.scale(5) + headingLineHeight * Math.max(1, headingLines.size());

        if (detailed) {
            FontMetrics instructionMetrics = graphics.getFontMetrics(baseFont);
            List<String> instructionLines = wrapText(instructionTextFor(hint), instructionMetrics, hintMaxTextWidth());
            int instructionLineHeight = instructionMetrics.getHeight();
            // instructionBaseline starts at headingBaseline - headingLineHeight + 22, then advances per line.
            offset = offset - headingLineHeight + JBUI.scale(22) + instructionLineHeight * Math.max(1, instructionLines.size());
        }
        // Breathing room (covers font descent below the last baseline, plus a visible gap) before whatever's
        // rendered next - without this the next flow's box would start flush against the last line of text.
        return offset + JBUI.scale(15);
    }

    /**
     * Break text into lines, each no wider than maxWidth when rendered with the given metrics, breaking only at
     * word boundaries. A single word wider than maxWidth is kept whole on its own line rather than split.
     */
    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    public static void markModuleLaunched(Project project) {
        PropertiesComponent.getInstance(project).setValue(MODULE_LAUNCHED_PROPERTY, true);
        repaintCanvas(project);
    }

    public static void markConsoleOpened(Project project) {
        PropertiesComponent.getInstance(project).setValue(CONSOLE_OPENED_PROPERTY, true);
        repaintCanvas(project);
    }

    private static void repaintCanvas(Project project) {
        DesignerCanvas canvas = project.getService(UiContext.class).getDesignerCanvas();
        if (canvas != null) {
            canvas.repaint();
        }
    }

    public void setInitialiseAllDimensions(boolean initialiseAllDimensions) {
        this.initialiseAllDimensions = initialiseAllDimensions;
    }

    public void saveAsImage(File file, String imageFormat, boolean transparentBackground) {
        int imageType = transparentBackground ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage bufferedImage = ImageUtil.createImage(getWidth(), getHeight(), imageType);
        Graphics graphics = bufferedImage.getGraphics();
        if (!transparentBackground) {
            graphics.setColor(getThemeAwareBackgroundColor());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        paint(graphics);
        try {
            boolean saved = ImageIO.write(bufferedImage, imageFormat, file);
            if (!saved) {
                StudioUIUtils.displayErrorMessage(project, StudioBundle.message("message.CouldNotSaveFile", file.getAbsolutePath()));
            } else {
                StudioUIUtils.displayMessage(project, StudioBundle.message("message.SavedFileTo", file.getAbsolutePath()));
            }
        } catch (IOException ioe) {
            StudioUIUtils.displayErrorMessage(project, StudioBundle.message("message.CouldNotSaveImageToFile", file.getAbsolutePath()));
            LOG.warn("STUDIO: Error saving image to file " + file.getAbsolutePath(), ioe);
        }
    }

    private static final class ErrorDetailsDialog extends DialogWrapper {
        private final String report;
        private final JBTextArea detailsArea;
        private final Action copyAllAction = new AbstractAction(StudioBundle.message("button.CopyAll")) {
            @Override
            public void actionPerformed(ActionEvent event) {
                CopyPasteManager.getInstance().setContents(new StringSelection(report));
            }
        };

        private ErrorDetailsDialog(Project project, String flowName, String report) {
            super(project, false);
            this.report = report;
            this.detailsArea = new JBTextArea(report, 28, 100);
            setModal(false);
            setTitle(StudioBundle.message("dialog.FlowErrorDetails", flowName));
            setOKButtonText(StudioBundle.message("button.Close"));
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            detailsArea.setEditable(false);
            detailsArea.setLineWrap(false);
            detailsArea.setCaretPosition(0);
            detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, StudioUIUtils.getMainFont().getSize()));
            detailsArea.getAccessibleContext().setAccessibleName(StudioBundle.message("label.FlowErrorDetails"));
            JBScrollPane scrollPane = new JBScrollPane(detailsArea);
            scrollPane.setPreferredSize(JBUI.size(850, 500));
            return scrollPane;
        }

        @Override
        protected Action[] createActions() {
            return new Action[]{copyAllAction, getOKAction()};
        }
    }

    public Module getIkasanModule() {
        return project.getService(UiContext.class).getIkasanModule();
    }
}
