package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.Pair;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolverPanel;
import org.ikasan.studio.ui.component.properties.PropertiesDialogue;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.ikasan.studio.ui.UiContext.JSON_MODEL_FILE_WITH_EXTENSION;

/**
 * The main painting / design panel
 */
public class DesignerCanvas extends JPanel {
    private static final Logger LOG = Logger.getInstance("#DesignerCanvas");
    private boolean initialiseAllDimensions = true;
    private boolean drawGrid = false;
    private int clickStartMouseX = 0 ;
    private int clickStartMouseY = 0 ;
    private boolean screenChanged = false;
    private final String projectKey ;
    private final JButton startButton = new JButton("Choose metapack then click here");
    private final JComboBox<Object> metaDataVersionJComboBox;

    public DesignerCanvas(String projectKey) {
        this.projectKey = projectKey;
        setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        setBackground(JBColor.WHITE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LOG.trace("Mouse press x "+ e.getX() + " y " + e.getY());
                mouseClickAction(e, e.getX(),e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                LOG.trace("Mouse release click x "+ e.getX() + " y " + e.getY());
                mouseReleaseAction();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                LOG.trace("DesignerCanvas listening to mouse drag x " + e.getX() + " y " + e.getY());
                mouseDragAction(e.getX(),e.getY());
            }
        });
        if (UiContext.getOptions(projectKey).isHintsEnabled()) {
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    LOG.trace("DesignerCanvas listening to mouse move x " + e.getX() + " y " + e.getY());
                mouseMoveAction(e.getX(),e.getY());
                }
            });
        }
        setTransferHandler(new CanvasImportTransferHandler( this));

        List<String> installedMetapacks = IkasanComponentLibrary.getMetapackList();
        metaDataVersionJComboBox = new JComboBox<>(installedMetapacks.toArray());
        // Create the properties popup panel for a new Module
        startButton.addActionListener(e ->
            {
                String metapackVersion = (String) metaDataVersionJComboBox.getSelectedItem();
                if (metapackVersion == null) {
                    StudioUIUtils.displayIdeaInfoMessage(projectKey, "A module can't be created until at least one meta-pack is loaded.");
                } else {
                    try {
                        UiContext.setIkasanModule(projectKey, Module.moduleBuilder().version(metapackVersion).build());
                    } catch (StudioBuildException ex) {
                        throw new RuntimeException(ex);
                    }
                    // Intellij startup is multi-threaded so caution is required.
                    if (UiContext.getPalettePanel(projectKey) != null) {
                        UiContext.getPalettePanel(projectKey).resetPallette();
                    }
                    ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
                    componentPropertiesPanel.updateTargetComponent(getIkasanModule());
                    PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                            UiContext.getProject(projectKey),
                            UiContext.getDesignerCanvas(projectKey),
                            componentPropertiesPanel);
                    if (propertiesDialogue.showAndGet()) {
                        PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
                        pipsiIkasanModel.generateJsonFromModelInstance();
                        pipsiIkasanModel.generateSourceFromModelInstance3();
                        disableModuleInitialiseProcess();
                    }
                }
            }
        );
    }

    public void enableModuleInitialiseProcess() {
        if (startButton.getParent() != this) {
            this.add(startButton);
            this.add(metaDataVersionJComboBox);
        }
    }

    public void disableModuleInitialiseProcess() {
        if (startButton != null) {
            this.remove(startButton);
        }
        if (metaDataVersionJComboBox != null) {
            this.remove(metaDataVersionJComboBox);
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
        BasicElement mouseSelectedComponent = getComponentAtXY(x, y);

          // Right click - popup menus
        if (me.getButton() == MouseEvent.BUTTON3) {
            if (mouseSelectedComponent != null) {
                DesignCanvasContextMenu.showPopupAndNavigateMenu(projectKey, this, me, mouseSelectedComponent);
            } else {
                DesignCanvasContextMenu.showPopupMenu(projectKey,this, me);
            }
        } // Double click -> go to source
        else if (mouseSelectedComponent != null && me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2 && ! me.isConsumed()) {
            me.consume();
            AbstractViewHandlerIntellij viewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, mouseSelectedComponent);
            if (viewHandler != null) {
                if (viewHandler.getOffsetInclassToNavigateTo() != 0) {
                    Navigator.navigateToSource(projectKey, viewHandler.getClassToNavigateTo(), viewHandler.getOffsetInclassToNavigateTo());
                } else {
                    if (viewHandler.getClassToNavigateTo() != null) {
                        Navigator.navigateToSource(projectKey, viewHandler.getClassToNavigateTo());
                    }
                }
            }
        } // Single click -> update properties
        else if ((me.getButton() == MouseEvent.BUTTON1) &&
                 (  mouseSelectedComponent != null &&
                    ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, mouseSelectedComponent) != null &&
                    ! ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, mouseSelectedComponent).isAlreadySelected()

                 )) {
            setSelectedComponent(mouseSelectedComponent);
            if (mouseSelectedComponent instanceof ExceptionResolver) {
                ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
                exceptionResolverPanel.updateTargetComponent(mouseSelectedComponent);
                PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                        UiContext.getProject(projectKey),
                        UiContext.getDesignerCanvas(projectKey),
                        exceptionResolverPanel);
                if (propertiesDialogue.showAndGet()) {
                    //@TODO MODEL
                    PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
                    pipsiIkasanModel.generateJsonFromModelInstance();
                    pipsiIkasanModel.generateSourceFromModelInstance3();
                }
            } else {
                UiContext.getPropertiesPanel(projectKey).updateTargetComponent(mouseSelectedComponent);
            }
        }
    }

    /**
     * This will be called to redraw the screen after the mouse movement is over, the mouse movement will allow the
     * component to move across the screen but not redraw the connectors, this will ensure all connectors and whole
     * screen is redrawn
     */
    private void mouseReleaseAction(){
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
        BasicElement mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        if (mouseSelectedComponent instanceof Flow && ((Flow)mouseSelectedComponent).getFlowIntegrityStatus() != null) {
            this.setToolTipText(((Flow)mouseSelectedComponent).getFlowIntegrityStatus());
        } else {
            this.setToolTipText("");
        }
    }

    /**
     * Called by the mouse drag listener
     * @param mouseX at the start of the drag
     * @param mouseY at the start of the drag
     */
    private void mouseDragAction(int mouseX, int mouseY) {
        BasicElement mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        LOG.trace("Mouse Motion listening x " + mouseX + " y " + mouseY + " component " + mouseSelectedComponent);

        if (mouseSelectedComponent instanceof FlowElement) {
            screenChanged = true;
            AbstractViewHandlerIntellij vh = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, mouseSelectedComponent);
            if (vh != null) {
                LOG.trace("Mouse drag start x[ " + clickStartMouseX + "] y " + clickStartMouseY + "] now  x [" + mouseX + "] y [" + mouseY +
                        "] Generator selected [" + mouseSelectedComponent.getComponentName() + "] x [" + vh.getLeftX() + "] y [" + vh.getTopY() + "] ");

                final int componentX = vh.getLeftX();
                final int componentY = vh.getTopY();
                final int deltaX = mouseX - clickStartMouseX;
                final int deltaY = mouseY - clickStartMouseY;

                if (deltaX != 0 || deltaY != 0) {
                    repaint(componentX, componentY, vh.getWidth(), vh.getHeight());
                    // Update coordinates.
                    vh.setLeftX(componentX + deltaX);
                    vh.setTopY(componentY + deltaY);

                    // Repaint the square at the new location.
                    repaint(vh.getLeftX(), vh.getTopY(), vh.getWidth(), vh.getHeight());
                    clickStartMouseX = mouseX;
                    clickStartMouseY = mouseY;
                }
            }
        }
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
                    .flatMap(x -> x.getFlowRoute().ftlGetConsumerAndFlowElements().stream())
                    .filter(x -> x.equals(ikasanBasicElement))
                    .peek(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).setAlreadySelected(true));
        } else if (ikasanBasicElement instanceof Flow) {
            ikasanModule.getFlows()
                    .stream()
                    .filter(x -> x.equals(ikasanBasicElement))
                    .peek(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).setAlreadySelected(true));
        } else {
            ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanModule).setAlreadySelected(true);
        }
    }


    /**
     * Ensure everything is deselected.
     */
    private void deSelectAllComponentsAndFlows() {
        Module ikasanModule = getIkasanModule();
        ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanModule).setAlreadySelected(false);
        ikasanModule.getFlows()
                .stream()
                .peek(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).setAlreadySelected(false))
                .flatMap(x -> x.getFlowRoute().ftlGetConsumerAndFlowElements().stream())
                .filter(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).isAlreadySelected())
                .peek(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).setAlreadySelected(false));
    }

    /**
     * Given the x and y coords, return the ikasan elements that resides at that x,y.
     * This will either be an ikasan flows component, an ikasan flow or the whole module.
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public BasicElement getComponentAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        BasicElement ikasanComponent = null;
        if (ikasanModule != null) {
            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowRoute().ftlGetConsumerAndFlowElements().stream())
                    .filter(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getLeftX() <= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getRightX() >= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getTopY() <= ypos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
        }
        if (ikasanComponent == null) {
            ikasanComponent =  getFlowExceptionResolverAtXY(xpos, ypos);
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
     *
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
                    .filter(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x.getExceptionResolver()).getLeftX() <= xpos &&
                            ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x.getExceptionResolver()).getRightX() >= xpos &&
                            ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x.getExceptionResolver()).getTopY() <= ypos &&
                            ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x.getExceptionResolver()).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);

            if (ikasanComponent != null) {
                ikasanComponent = ((Flow) ikasanComponent).getExceptionResolver();
            }
        }

        return ikasanComponent;
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
                    .filter(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getLeftX() <= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getRightX() >= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getTopY() <= ypos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
        }
        return ikasanComponent;
    }

    /**
     * Given the x and y coords, return true if there is an ikasan flow that resides at that x,y.
     *
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return true if an ikasan flow resides at the location, false otherwise
     */
    public boolean isFlowAtXY(int xpos, int ypos) {
        return null != getFlowAtXY(xpos, ypos);
    }

    /**
     * The transferHandler has indicated we are over a flow, decide how we will highlight that flow
     */
    public void componentDraggedToFlowAction(int mouseX, int mouseY, final BasicElement ikasanBasicElement) {
        if (ikasanBasicElement != null) {
            final BasicElement targetComponent = getComponentAtXY(mouseX, mouseY);
            Flow targetFlow = null;
            FlowElement targetFlowComponent;

            if (targetComponent instanceof FlowElement) {
                targetFlowComponent = (FlowElement)targetComponent;
                targetFlow = targetFlowComponent.getContainingFlow();
            } else if (targetComponent instanceof Flow) {
                targetFlow = (Flow)targetComponent;
            }

            if (targetFlow != null) {


                IkasanFlowViewHandler ikasanFlowViewHandler = ViewHandlerFactoryIntellij.getFlowViewHandler(projectKey, targetFlow);
                String issue = targetFlow.getFlowRoute().issueCausedByAdding(ikasanBasicElement.getComponentMeta());
                if (issue.isEmpty()) {
                    if (!ikasanFlowViewHandler.isFlowReceptiveMode()) {
                        ikasanFlowViewHandler.setFlowReceptiveMode();
                        this.repaint();
                    }
                } else {
                    if (!ikasanFlowViewHandler.isFlowWarningMode()) {
                        ikasanFlowViewHandler.setFlowlWarningMode(mouseX, mouseY, issue);
                        this.repaint();
                    }
                }
            } else {
                resetContextSensitiveHighlighting();
            }
        }
    }

    public void resetContextSensitiveHighlighting() {
        Module ikasanModule = getIkasanModule();
        boolean redrawNeeded = ikasanModule.getFlows()
                .stream()
                .anyMatch(x -> ! ((IkasanFlowViewHandler) ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x)).isFlowNormalMode());

        ikasanModule.getFlows().forEach(x -> ((IkasanFlowViewHandler) ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x)).setFlowNormalMode());
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
                for (FlowElement ikasanFlowComponent : flow.getFlowRoute().ftlGetConsumerAndFlowElements()) {
                    Proximity draggedToComponent = Proximity.getRelativeProximity(dragged, ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanFlowComponent).getCentrePoint(), proximityDetect);
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
        Module ikasanModule = getIkasanModule();
        if (x >= 0 && y >= 0) {
            BasicElement targetElement = getComponentAtXY(x,y);
            IkasanObject newComponent;
            // Add new component to existing flow
            if (targetElement instanceof FlowElement || targetElement instanceof Flow) {
                Flow containingFlow;
                if (targetElement instanceof Flow) {
                    containingFlow = (Flow)targetElement;
                } else {
                    containingFlow = ((FlowElement)targetElement).getContainingFlow();
                }
                if (!containingFlow.getFlowRoute().isValidToAdd(ikasanComponentType)) {
                    resetContextSensitiveHighlighting();
                    return false;
                }
                newComponent = null;
                try {
                    newComponent = createViableFlowComponent(ikasanComponentType, containingFlow);
                } catch (Exception ex) {
                    // Any exceptions raised here were silently handled, now exposed at least as logs
                    Thread thread = Thread.currentThread();
                    LOG.warn("ERROR: Intercept silent popup box failure, " + ex + " trace: " + Arrays.toString(ex.getStackTrace()));
                }
                if (newComponent != null) {
                    if (newComponent instanceof ExceptionResolver) {
                        containingFlow.setExceptionResolver((ExceptionResolver) newComponent);
                    } else {
                        if (newComponent.getComponentMeta().isConsumer()) {
                            containingFlow.setConsumer((FlowElement) newComponent);
                        } else {
                            insertNewComponentBetweenSurroundingPair(containingFlow, (FlowElement) newComponent, x, y);
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
                            .metapackVersion(UiContext.getIkasanModule(projectKey).getMetaVersion())
                            .build());
                } catch (StudioBuildException e) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "There was a problem trying to get meta pack info, please review logs (" + e.getMessage() + ")");
                    return false;
                }
                if (newComponent != null) {
                    ikasanModule.addFlow((Flow) newComponent);
                } else {
                    return false;
                }
            }
            PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
            pipsiIkasanModel.generateJsonFromModelInstance();
            pipsiIkasanModel.generateSourceFromModelInstance3();
            try {
                StudioPsiUtils.generateModelInstanceFromJSON(projectKey, false);
            } catch (StudioBuildException se) {
                LOG.warn("SERIOUS ERROR: Reported when reading " + JSON_MODEL_FILE_WITH_EXTENSION + Arrays.asList(se.getStackTrace()));
                StudioUIUtils.displayIdeaErrorMessage(projectKey, "Please fix " + JSON_MODEL_FILE_WITH_EXTENSION + " then use the Refresh Button");
                // The dumb module should contain just enough to prevent the plugin from crashing
                UiContext.setIkasanModule(projectKey, Module.getDumbModuleVersion());
            }

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
     * @return the fully populated component or null if the action was cancelled.
     */
    private FlowElement createViableFlowComponent(ComponentMeta ikasanComponentType, Flow containingFlow) {
        FlowElement newComponent = null;
        try {
            newComponent = FlowElementFactory.createFlowElement(UiContext.getIkasanModule(projectKey).getMetaVersion(), ikasanComponentType, containingFlow, null);
        } catch (StudioBuildException e) {
            StudioUIUtils.displayIdeaWarnMessage(projectKey, "There was a problem trying to get meta pack info, please review logs (" + e.getMessage() + ")");
            return newComponent;
        }
        if (ikasanComponentType.isExceptionResolver()) {
            return (FlowElement)createExceptionResolver((ExceptionResolver)newComponent);
        } else {
            return (FlowElement)createViableComponent(newComponent);
        }
    }

    /**
     * Create the popup properties panel for a new component
     * @param newComponent to be included in panel
     * @return the populated component or null if the action was cancelled.
     */
    private BasicElement createViableComponent(BasicElement newComponent) {
        if (newComponent.hasUnsetMandatoryProperties()) {
            // Add new component
            ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
            componentPropertiesPanel.updateTargetComponent(newComponent);
            PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                    UiContext.getProject(projectKey),
                    UiContext.getDesignerCanvas(projectKey),
                    componentPropertiesPanel);
            if (! propertiesDialogue.showAndGet()) {
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

            ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
            exceptionResolverPanel.updateTargetComponent(newExceptionResolver);
            PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                    UiContext.getProject(projectKey),
                    UiContext.getDesignerCanvas(projectKey),
                    exceptionResolverPanel);
            if (! propertiesDialogue.showAndGet()) {
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
    private void insertNewComponentBetweenSurroundingPair(Flow containingFlow, FlowElement ikasanFlowComponent, int x, int y) {
        if (ikasanFlowComponent.getComponentMeta().isConsumer()) {
            containingFlow.setConsumer(ikasanFlowComponent);
        } else {
            // insert new component between surrounding pair
            Pair<FlowElement, FlowElement> surroundingComponents = getSurroundingComponents(x, y);
            List<FlowElement> components = containingFlow.getFlowRoute().getFlowElements() ;
            int numberOfComponents = components.size();
            if (numberOfComponents == 0) {
                components.add(ikasanFlowComponent);
            } else {
                for (int ii = 0 ; ii < numberOfComponents ; ii++ ) {
                    if (components.get(ii).equals(surroundingComponents.getRight())) {
                        components.add(ii, ikasanFlowComponent);
                        break;
                    } else if (components.get(ii).equals(surroundingComponents.getLeft())) {
                        components.add(ii+1, ikasanFlowComponent);
                        break;
                    }
                }
            }
        }
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
        LOG.debug("paintComponent invoked");
        super.paintComponent(g);

        if (ikasanModule != null) {
            AbstractViewHandlerIntellij moduleViewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanModule);
            if (initialiseAllDimensions && moduleViewHandler != null) {
                moduleViewHandler.initialiseDimensions(g, 0, 0, this.getWidth(), this.getHeight());
                initialiseAllDimensions = false;
            }

            if (ikasanModule.hasUnsetMandatoryProperties()) {
                LOG.warn("Ikasan Module is not in the context, assuming this is a new buildRouteTree");
                enableModuleInitialiseProcess();
            }
            if (moduleViewHandler != null) {
                moduleViewHandler.paintComponent(this, g, -1, -1);
            }
        }
        if (drawGrid) {
            StudioUIUtils.paintGrid(g, getWidth(), getHeight());
        }
    }

    public void setInitialiseAllDimensions(boolean initialiseAllDimensions) {
        this.initialiseAllDimensions = initialiseAllDimensions;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    public void saveAsImage(File file, String imageFormat, boolean transparentBackground) {
        int imageType = transparentBackground ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage bufferedImage = ImageUtil.createImage(getWidth(), getHeight(), imageType);
        Graphics graphics = bufferedImage.getGraphics();
        if (!transparentBackground) {
            graphics.setColor(JBColor.WHITE);
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        paint(graphics);
        try {
            boolean saved = ImageIO.write(bufferedImage, imageFormat, file);
            if (!saved) {
                StudioUIUtils.displayErrorMessage(projectKey, "Could not save file " + file.getAbsolutePath());
            } else {
                StudioUIUtils.displayMessage(projectKey, "Saved file to " + file.getAbsolutePath());
            }
        } catch (IOException ioe) {
            StudioUIUtils.displayErrorMessage(projectKey, "Could not save image to file " + file.getAbsolutePath());
            LOG.warn("Error saving image to file " + file.getAbsolutePath(), ioe);
        }
    }

    public Module getIkasanModule() {
        return UiContext.getIkasanModule(projectKey);
    }
}
