package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.Pair;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolverPanel;
import org.ikasan.studio.ui.component.properties.PropertiesPopupDialogue;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        setBackground(JBColor.WHITE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LOG.trace("STUDIO: Mouse press x "+ e.getX() + " y " + e.getY());
                mouseClickAction(e, e.getX(),e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                LOG.trace("STUDIO: Mouse release click x "+ e.getX() + " y " + e.getY());
                mouseReleaseAction();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                LOG.trace("STUDIO: DesignerCanvas listening to mouse drag x " + e.getX() + " y " + e.getY());
                mouseDragAction(e.getX(),e.getY());
            }
        });
        if (UiContext.getOptions(projectKey).isHintsEnabled()) {
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
        metaDataVersionJComboBox = new JComboBox<>(installedMetapacks.toArray());
        // Create the properties popup panel for a new Module
        startButton.addActionListener(e ->
            {
                String metapackVersion = (String) metaDataVersionJComboBox.getSelectedItem();
                if (metapackVersion == null) {
                    StudioUIUtils.displayIdeaInfoMessage(projectKey, "A module can't be created until at least one meta-pack is loaded.");
                } else {
                    Module module = UiContext.getIkasanModule(projectKey);
                    if (module == null) {
                        try {
                            UiContext.setIkasanModule(projectKey, Module.moduleBuilder().version(metapackVersion).build());
                        } catch (StudioBuildException ex) {
                            throw new RuntimeException(ex);
                        }
                    } else {
                        module.setVersion(metapackVersion);
                    }
                    // Intellij startup is multi-threaded so caution is required.
                    if (UiContext.getPalettePanel(projectKey) != null) {
                        UiContext.getPalettePanel(projectKey).resetPallette();
                    }
                    ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
                    componentPropertiesPanel.updateTargetComponent(getIkasanModule());
                    PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                            UiContext.getProject(projectKey),
                            UiContext.getDesignerCanvas(projectKey),
                            componentPropertiesPanel);
                    if (propertiesPopupDialogue.showAndGet()) {
                        StudioUIUtils.displayIdeaInfoMessage(projectKey, "Please wait for Intellij to initialise, any code errors will be resolved.");
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
            this.add(metaDataVersionJComboBox);
            this.add(startButton);
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
        IkasanComponent mouseSelectedComponent = getComponentAtXY(x, y);

        if (!(mouseSelectedComponent instanceof BasicElement basicElement)) {
            return;
        }
        // Right click - popup menus
        if (me.getButton() == MouseEvent.BUTTON3) {
            DesignCanvasContextMenu.showPopupAndNavigateMenu(projectKey, this, me, basicElement);
//            if (mouseSelectedComponent != null) {
//            } else {
//                DesignCanvasContextMenu.showPopupMenu(projectKey,this, me);
//            }
        } // Double click -> go to source
        else if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2 && ! me.isConsumed()) {
            me.consume();
            AbstractViewHandlerIntellij viewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, basicElement);
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
                 (  ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, basicElement) != null &&
                    ! ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, basicElement).isAlreadySelected()

                 )) {
            setSelectedComponent(basicElement);
            if (mouseSelectedComponent instanceof ExceptionResolver) {
                ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
                exceptionResolverPanel.updateTargetComponent(basicElement);
                PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                        UiContext.getProject(projectKey),
                        UiContext.getDesignerCanvas(projectKey),
                        exceptionResolverPanel);
                if (propertiesPopupDialogue.showAndGet()) {
                    //@TODO MODEL
                    PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
                    pipsiIkasanModel.generateJsonFromModelInstance();
                    pipsiIkasanModel.generateSourceFromModelInstance3();
                }
            } else {
                UiContext.getPropertiesTabPanel(projectKey).updateTargetComponent(basicElement);
                UiContext.getPropertiesPanel(projectKey).updateTargetComponent(basicElement);
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
        IkasanComponent mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        if (mouseSelectedComponent instanceof Flow && ((Flow) mouseSelectedComponent).getFlowIntegrityStatus() != null) {
            this.setToolTipText(((Flow) mouseSelectedComponent).getFlowIntegrityStatus());
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
        IkasanComponent mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        LOG.trace("STUDIO: Mouse Motion listening x " + mouseX + " y " + mouseY + " component " + mouseSelectedComponent);

        if (mouseSelectedComponent instanceof FlowElement) {
            FlowElement flowElement = (FlowElement) mouseSelectedComponent;
            screenChanged = true;
            AbstractViewHandlerIntellij vh = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, flowElement);
            if (vh != null) {
                LOG.trace("STUDIO: Mouse drag start x[ " + clickStartMouseX + "] y " + clickStartMouseY + "] now  x [" + mouseX + "] y [" + mouseY +
                        "] Generator selected [" + flowElement.getComponentName() + "] x [" + vh.getLeftX() + "] y [" + vh.getTopY() + "] ");

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
                    .flatMap(x -> x.getFlowRoute().getConsumerAndFlowRouteElements().stream())
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
                .flatMap(x -> x.getFlowRoute().getConsumerAndFlowRouteElements().stream())
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
    public IkasanComponent getComponentAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        IkasanComponent ikasanComponent = null;
        if (ikasanModule != null) {
            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowElementsNoExternalEndPoints().stream())
                    .filter(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getLeftX() <= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getRightX() >= xpos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getTopY() <= ypos && ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x).getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
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
     * Given the x and y coords, return the ikasan flow route that resides at that x,y.
     *
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public IkasanComponent getFlowRouteAtXY(int xpos, int ypos) {
        Module ikasanModule = getIkasanModule();
        IkasanComponent ikasanComponent = null;
        if (ikasanModule != null) {
            IkasanFlowRouteViewHandler ikasanFlowRouteViewHandler = ikasanModule.getFlows()
                    .stream()
                    .map(x -> ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, x))
                    .map(x -> ((IkasanFlowViewHandler)x))
                    .flatMap(x -> x.getFlowRouteViewHandler().getAllFlowRouteViewHandlers(new ArrayList<>(), x.getFlowRouteViewHandler()).stream())
                    .filter(x -> x.getLeftX() <= xpos && x.getRightX() >= xpos && x.getTopY() <= ypos && x.getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
            if (ikasanFlowRouteViewHandler != null) {
                ikasanComponent = ikasanFlowRouteViewHandler.getFlowRoute();
            }
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

            if ((ikasanBasicElement.getComponentMeta().isDebug() && targetElement instanceof FlowElement && !((FlowElement)targetElement).getComponentMeta().isConsumer()) ||
                (!ikasanBasicElement.getComponentMeta().isDebug() && (targetFlowRoute != null || targetFlow != null))) {

                LOG.info("Taget element was " + targetElement);

                String issue = "";
                if (targetFlowRoute != null) {
                    issue = targetFlowRoute.issueCausedByAdding(ikasanBasicElement.getComponentMeta());
                }
                if (targetFlow != null) {
                    issue += targetFlow.issueCausedByAdding(ikasanBasicElement.getComponentMeta(), targetFlowRoute);
                    IkasanFlowViewHandler ikasanFlowViewHandler = ViewHandlerFactoryIntellij.getOrCreateFlowViewHandler(projectKey, targetFlow);
                    if (issue.isEmpty()) {
                        okToAdd = true;
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
                }
            } else {
                resetContextSensitiveHighlighting();
            }
        }
        return okToAdd;
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
                for (FlowElement ikasanFlowComponent : flow.getFlowElementsNoExternalEndPoints()) {
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
            IkasanComponent targetElement = getComponentAtXY(x,y);
            IkasanObject newComponent;
            if (targetElement instanceof FlowElement || targetElement instanceof Flow || targetElement instanceof FlowRoute) {
                Flow containingFlow;
                FlowRoute containingFlowRoute = null;
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

                //  If the add is not allowed, return false.
                if (containingFlowRoute != null && ! containingFlowRoute.isValidToAdd(ikasanComponentType)) {
                    resetContextSensitiveHighlighting();
                    return false;
                }
                if (containingFlow != null && !containingFlow.getFlowRoute().isValidToAdd(ikasanComponentType)) {
                    resetContextSensitiveHighlighting();
                    return false;
                }

                newComponent = null;
                try {
                    newComponent = createViableFlowComponent(ikasanComponentType, containingFlow, containingFlowRoute);
                } catch (Exception ex) {
                    // Any exceptions raised here were silently handled, now exposed at least as logs
                    LOG.warn("ERROR: Intercept silent popup box failure, " + ex + " trace: " + Arrays.toString(ex.getStackTrace()));
                }
                if (newComponent != null) {
                    if (newComponent instanceof ExceptionResolver) {
                        containingFlow.setExceptionResolver((ExceptionResolver) newComponent);
                    } else {
                        ((FlowElement) newComponent).defaultUnsetMandatoryProperties();
                        insertNewComponentBetweenSurroundingPair(containingFlow, containingFlowRoute, (FlowElement) newComponent, x, y);
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
    private FlowElement createViableFlowComponent(ComponentMeta ikasanComponentType, Flow containingFlow, FlowRoute containingFlowFoute) {
        FlowElement newComponent = null;
        try {
            newComponent = FlowElementFactory.createFlowElement(UiContext.getIkasanModule(projectKey).getMetaVersion(), ikasanComponentType, containingFlow, containingFlowFoute, null);
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
        // Now this is a serious components, ensure any property with tag placeholder are updated to real values
        if (newComponent instanceof FlowElement) {
            FlowElement newFlowComponent = (FlowElement)newComponent;
            StudioBuildUtils.substituteAllPlaceholderInPascalCase(UiContext.getIkasanModule(projectKey), newFlowComponent.getContainingFlow(), newFlowComponent);
        }

        if (newComponent.getComponentMeta().isDebug()) {
            FlowElement newFlowComponent = (FlowElement)newComponent;
            newFlowComponent.defaultUnsetMandatoryProperties();
        }

        if (newComponent.hasUnsetMandatoryProperties()) {
            // Add new component
            ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
            componentPropertiesPanel.updateTargetComponent(newComponent);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    UiContext.getProject(projectKey),
                    UiContext.getDesignerCanvas(projectKey),
                    componentPropertiesPanel);
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

            ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
            exceptionResolverPanel.updateTargetComponent(newExceptionResolver);
            PropertiesPopupDialogue propertiesPopupDialogue = new PropertiesPopupDialogue(
                    UiContext.getProject(projectKey),
                    UiContext.getDesignerCanvas(projectKey),
                    exceptionResolverPanel);
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

                if (ikasanFlowComponent.getComponentMeta().isDebug() && surroundingComponents.getRight() != null) {
                    ikasanFlowComponent.setName(surroundingComponents.getRight().getName()+"Debug");
                    String rawClassname = ikasanFlowComponent.getPropertyValueAsString(USER_IMPLEMENTED_CLASS_NAME);
                    String substitutedClassname = substitutePlaceholderInPascalCase(getIkasanModule(), containingFlow, ikasanFlowComponent, rawClassname);
                    ikasanFlowComponent.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, substitutedClassname);
                }
                if (targetRoute != null) {
                    List<FlowElement> components = targetRoute.getFlowElements();
                    int numberOfComponents = components.size();

                    if (numberOfComponents == 0) {
                        components.add(0, ikasanFlowComponent);
                    } else {
                        for (int ii = 0; ii < numberOfComponents; ii++) {
                            if (components.get(ii).equals(surroundingComponents.getRight()) ||
                                    ((components.get(ii).equals(surroundingComponents.getLeft())) && surroundingComponents.getLeft().getComponentMeta().isProducer())) {
                                components.add(ii, ikasanFlowComponent);
                                break;
                            } else if (components.get(ii).equals(surroundingComponents.getLeft())) {
                                components.add(ii + 1, ikasanFlowComponent);
                                break;
                            }
                        }
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
//LOG.info("1 StudioXX new dimenstions dimension:" + this.getPreferredSize());

        Module ikasanModule = getIkasanModule();
        if (ikasanModule != null && ikasanModule.isInitialised()) {
            disableModuleInitialiseProcess();
        } else {
            enableModuleInitialiseProcess();
        }
        if (ikasanModule != null) {
            AbstractViewHandlerIntellij moduleViewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanModule);
            if (initialiseAllDimensions && moduleViewHandler != null) {
                moduleViewHandler.initialiseDimensions(g, 0, 0, this.getWidth(), this.getHeight());
                initialiseAllDimensions = false;
            }

            if (ikasanModule.hasUnsetMandatoryProperties()) {
                LOG.warn("STUDIO: Ikasan Module is not in the context, assuming this is a new buildRouteTree");
                enableModuleInitialiseProcess();
            }
            int newWidth = moduleViewHandler.getWidth() + 0;
            int newHeight = moduleViewHandler.getHeight() + 0;
            this.setPreferredSize(new Dimension(newWidth, newHeight));
            revalidate();
            super.paintComponent(g);

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
            LOG.warn("STUDIO: Error saving image to file " + file.getAbsolutePath(), ioe);
        }
    }

    public Module getIkasanModule() {
        return UiContext.getIkasanModule(projectKey);
    }
}
