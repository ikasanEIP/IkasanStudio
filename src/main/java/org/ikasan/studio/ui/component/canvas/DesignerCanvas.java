package org.ikasan.studio.ui.component.canvas;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.Pair;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.ikasan.*;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolverPanel;
import org.ikasan.studio.ui.component.properties.PropertiesDialogue;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.ikasan.studio.model.ikasan.IkasanComponentType.EXCEPTION_RESOLVER;

/**
 * The main painting / design panel
 */
public class DesignerCanvas extends JPanel {
    private boolean initialiseAllDimensions = true;
    private boolean drawGrid = false;
    private static final Logger log = Logger.getLogger(DesignerCanvas.class);
    private transient IkasanModule ikasanModule ;       // Old Swing serialisation not required.
    private int clickStartMouseX = 0 ;
    private int clickStartMouseY = 0 ;
    private boolean screenChanged = false;
    private String projectKey ;
    private JButton startButton = new JButton("Click here to start");

    public DesignerCanvas(String projectKey) {
        this.projectKey = projectKey;
        ikasanModule = Context.getIkasanModule(projectKey);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setBackground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                log.trace("Mouse press x "+ e.getX() + " y " + e.getY());
                mouseClickAction(e, e.getX(),e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                log.trace("Mouse release click x "+ e.getX() + " y " + e.getY());
                mouseReleaseAction();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                log.trace("DesignerCanvas listening to mouse drag x " + e.getX() + " y " + e.getY());
                mouseDragAction(e.getX(),e.getY());
            }
        });
        if (Context.getOption(projectKey).isHintsEnabled()) {
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                log.trace("DesignerCanvas listening to mouse move x " + e.getX() + " y " + e.getY());
                mouseMoveAction(e.getX(),e.getY());
                }
            });
        }
        setTransferHandler(new CanvasImportTransferHandler( this));

        // Create the properties popup panel for a new Module
        startButton.addActionListener(e ->
            {
                ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
                componentPropertiesPanel.updateTargetComponent(ikasanModule);
                PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                        Context.getProject(projectKey),
                        Context.getDesignerCanvas(projectKey),
                        componentPropertiesPanel);
                if (propertiesDialogue.showAndGet()) {
                    PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
                    pipsiIkasanModel.generateSourceFromModel(ikasanModule.getType().getComponentDependency().getDependencies());
                    disableStart();
                }
            }
        );
    }

    public void enableStart() {
        if (startButton.getParent() != this) {
            this.add(startButton);
        }
    }

    public void disableStart() {
        this.remove(startButton);
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
            if (mouseSelectedComponent.getViewHandler().getOffsetInclassToNavigateTo() != 0) {
                Navigator.navigateToSource(projectKey, mouseSelectedComponent.getViewHandler().getClassToNavigateTo(), mouseSelectedComponent.getViewHandler().getOffsetInclassToNavigateTo());
            } else {
                if (mouseSelectedComponent.getViewHandler().getClassToNavigateTo() != null) {
                    Navigator.navigateToSource(projectKey, mouseSelectedComponent.getViewHandler().getClassToNavigateTo());
                }
            }
        } // Single click -> update properties
        else if ((me.getButton() == MouseEvent.BUTTON1) &&
                 (mouseSelectedComponent != null && !mouseSelectedComponent.getViewHandler().isAlreadySelected())) {
            setSelectedComponent(mouseSelectedComponent);
            if (mouseSelectedComponent instanceof IkasanExceptionResolver) {
                // XXXXXX
                ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
                exceptionResolverPanel.updateTargetComponent(mouseSelectedComponent);
                PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                        Context.getProject(projectKey),
                        Context.getDesignerCanvas(projectKey),
                        exceptionResolverPanel);
                if (propertiesDialogue.showAndGet()) {
                    PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
                    pipsiIkasanModel.generateSourceFromModel(ikasanModule.getType().getComponentDependency().getDependencies());
                }
            } else {
                Context.getPropertiesPanel(projectKey).updateTargetComponent(mouseSelectedComponent);
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
        if (mouseSelectedComponent instanceof IkasanFlow && ((IkasanFlow)mouseSelectedComponent).getFlowIntegrityStatus() != null) {
            this.setToolTipText(((IkasanFlow)mouseSelectedComponent).getFlowIntegrityStatus());
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
        log.trace("Mouse Motion listening x " + mouseX + " y " + mouseY + " component " + mouseSelectedComponent);

        if (mouseSelectedComponent instanceof IkasanFlowComponent) {
            screenChanged = true;
            ViewHandler vh = mouseSelectedComponent.getViewHandler();
            log.trace("Mouse drag start x[ " + clickStartMouseX + "] y " + clickStartMouseY + "] now  x [" + mouseX + "] y [" + mouseY +
                    "] Generator selected [" + mouseSelectedComponent.getName() + "] x [" + vh.getLeftX() + "] y [" + vh.getTopY() + "] ");

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

    /**
     * We have clicked on the canvas.
     * If we are on a flow component, set that that the selected component
     * If we are on a flow but not over a flow component, make the flow the selected component
     * If we are on the  canvas and not any flow, set the module as the selected component
     * @param component currently pointed to by the mouse.
     */
    public void setSelectedComponent(IkasanComponent component) {
        deSelectAllCompnentsAndFlows();
        // Set selected
        if (component instanceof IkasanFlowComponent) {
            ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowComponentList().stream())
                    .filter(x -> x.equals(component))
                    .peek(x -> x.getViewHandler().setAlreadySelected(true));
        } else if (component instanceof IkasanFlow) {
            ikasanModule.getFlows()
                    .stream()
                    .filter(x -> x.equals(component))
                    .peek(x -> x.getViewHandler().setAlreadySelected(true));
        } else {
            ikasanModule.getViewHandler().setAlreadySelected(true);
        }
    }

    /**
     * Ensure everything is deselected.
     */
    private void deSelectAllCompnentsAndFlows() {
        ikasanModule.getViewHandler().setAlreadySelected(false);
        ikasanModule.getFlows()
                .stream()
                .peek(x -> x.getViewHandler().setAlreadySelected(false))
                .flatMap(x -> x.getFlowComponentList().stream())
                .filter(x -> x.getViewHandler().isAlreadySelected())
                .peek(x -> x.getViewHandler().setAlreadySelected(false));
    }

    /**
     * Given the x and y coords, return the ikasan elements that resides at that x,y.
     *
     * This will either be an ikasan flows component, an ikasan flow or the whol module.
     *
     * @param xpos of the mouse click
     * @param ypos of the mouse click
     * @return the ikasan component (flows component, flow, module) currently selected.
     */
    public IkasanComponent getComponentAtXY(int xpos, int ypos) {
        IkasanComponent ikasanComponent = null;
        if (ikasanModule != null) {
            ikasanComponent = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowComponentList().stream())
                    .filter(x -> x.getViewHandler().getLeftX() <= xpos && x.getViewHandler().getRightX() >= xpos && x.getViewHandler().getTopY() <= ypos && x.getViewHandler().getBottomY() >= ypos)
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
    public IkasanComponent getFlowExceptionResolverAtXY(int xpos, int ypos) {
        IkasanComponent ikasanComponent = null;
        ikasanComponent = ikasanModule.getFlows()
                .stream()
                .filter(x -> x.hasExceptionResolver())
                .filter(x -> x.getIkasanExceptionResolver().getViewHandler().getLeftX() <= xpos &&
                        x.getIkasanExceptionResolver().getViewHandler().getRightX() >= xpos &&
                        x.getIkasanExceptionResolver().getViewHandler().getTopY() <= ypos &&
                        x.getIkasanExceptionResolver().getViewHandler().getBottomY() >= ypos)
                .findFirst()
                .orElse(null);

        if (ikasanComponent != null) {
            ikasanComponent = ((IkasanFlow)ikasanComponent).getIkasanExceptionResolver();
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
    public IkasanComponent getFlowAtXY(int xpos, int ypos) {
        IkasanComponent ikasanComponent = null;
        ikasanComponent = ikasanModule.getFlows()
                .stream()
                .filter(x -> x.getViewHandler().getLeftX() <= xpos && x.getViewHandler().getRightX() >= xpos && x.getViewHandler().getTopY() <= ypos && x.getViewHandler().getBottomY() >= ypos)
                .findFirst()
                .orElse(null);

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
    public void componentDraggedToFlowAction(int mouseX, int mouseY, final IkasanFlowUIComponent ikasanFlowUIComponent) {
        if (ikasanFlowUIComponent != null) {
            final IkasanComponent targetComponent = getComponentAtXY(mouseX, mouseY);
            IkasanFlow targetFlow = null;
            IkasanFlowComponent targetFlowComponent = null;

            if (targetComponent instanceof IkasanFlowComponent) {
                targetFlowComponent = (IkasanFlowComponent)targetComponent;
                targetFlow = targetFlowComponent.getParent();
            } else if (targetComponent instanceof IkasanFlow) {
                targetFlow = (IkasanFlow)targetComponent;
            }

            if (targetFlow != null) {
                IkasanFlowViewHandler ikasanFlowViewHandler = (IkasanFlowViewHandler)targetFlow.getViewHandler();
                String issue = targetFlow.issueCausedByAdding(ikasanFlowUIComponent.getIkasanComponentType());
                if (issue.length() == 0) {
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
        boolean redrawNeeded = ikasanModule.getFlows()
                .stream()
                .anyMatch(x -> ! ((IkasanFlowViewHandler)x.getViewHandler()).isFlowNormalMode());

        ikasanModule.getFlows().forEach(x -> ((IkasanFlowViewHandler)x.getViewHandler()).setFlowNormalMode());
        if (redrawNeeded) {
            this.repaint();
        }
    }

    /**
     * Given the x and y coords, return ikasan elements to the left or right (or both) within reasonable bounds
     * @param xpos
     * @param ypos
     * @return
     */
    public Pair<IkasanFlowComponent,IkasanFlowComponent> getSurroundingComponents(int xpos, int ypos) {
        Pair<IkasanFlowComponent,IkasanFlowComponent> surroundingComponents = new Pair<>();
        Point dragged = new Point(xpos, ypos);
        Pair<Integer, Integer> proximityDetect = IkasanFlowComponentViewHandler.getProximityDetect();

        if (ikasanModule != null) {
            for (IkasanFlow flow : ikasanModule.getFlows()) {
                for (IkasanFlowComponent ikasanFlowComponent : flow.getFlowComponentList()) {
                    Proximity draggedToComponent = Proximity.getRelativeProximity(dragged, ikasanFlowComponent.getViewHandler().getCentrePoint(), proximityDetect);
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
    public boolean requestToAddComponent(int x, int y, IkasanComponentType ikasanComponentType) {
        if (x >= 0 && y >=0 && ikasanComponentType != null) {
            IkasanComponent ikasanComponent = getComponentAtXY(x,y);
            IkasanComponent newComponent = null;
            // Add new component to existing flow
            if (ikasanComponent instanceof  IkasanFlowComponent || ikasanComponent instanceof  IkasanFlow) {
                IkasanFlow containingFlow = null;
                if (ikasanComponent instanceof  IkasanFlow) {
                    containingFlow = (IkasanFlow)ikasanComponent;
                } else {
                    containingFlow = ((IkasanFlowComponent)ikasanComponent).getParent();
                }

                if (!containingFlow.isValidToAdd(ikasanComponentType)) {
                    resetContextSensitiveHighlighting();
                    return false;
                }

                newComponent = createViableFlowComponent(ikasanComponentType, containingFlow);
                if (newComponent != null) {
                    if (newComponent instanceof IkasanExceptionResolver) {
                        containingFlow.setIkasanExceptionResolver((IkasanExceptionResolver)newComponent);
                    } else {
                        insertNewComponentBetweenSurroundingPair(containingFlow, (IkasanFlowComponent) newComponent, x, y);
                    }
                } else {
                    return false;
                }
            } else {
                newComponent = createViableComponent(new IkasanFlow());
                if (newComponent != null) {
                    ikasanModule.addFlow((IkasanFlow) newComponent);
                } else {
                    return false;
                }
            }

            PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
            pipsiIkasanModel.generateSourceFromModel(newComponent.getType().getComponentDependency().getDependencies());
            StudioPsiUtils.generateModelFromSourceCode(projectKey, false);
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
    private IkasanFlowComponent createViableFlowComponent(IkasanComponentType ikasanComponentType, IkasanFlow containingFlow) {
        IkasanFlowComponent newComponent = IkasanFlowComponent.getInstance(ikasanComponentType, containingFlow);
        if (EXCEPTION_RESOLVER.equals(ikasanComponentType)) {
            return (IkasanFlowComponent)createExceptionResolver(newComponent);
        } else {
            return (IkasanFlowComponent)createViableComponent(newComponent);
        }
    }

    /**
     * Create the popup properties panel for a new component
     * @param newComponent
     * @return the populated component or null if the action was cancelled.
     */
    private IkasanComponent createViableComponent(IkasanComponent newComponent) {
        if (newComponent.hasUnsetMandatoryProperties()) {

            ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(projectKey, true);
            componentPropertiesPanel.updateTargetComponent(newComponent);
            PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                    Context.getProject(projectKey),
                    Context.getDesignerCanvas(projectKey),
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
     * @param newComponent
     * @return the populated component or null if the action was cancelled.
     */
    private IkasanComponent createExceptionResolver(IkasanComponent newComponent) {
        if (newComponent.hasUnsetMandatoryProperties()) {

            ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(projectKey, true);
            exceptionResolverPanel.updateTargetComponent(newComponent);
            PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                    Context.getProject(projectKey),
                    Context.getDesignerCanvas(projectKey),
                    exceptionResolverPanel);
            if (! propertiesDialogue.showAndGet()) {
                // i.e. cancel.
                newComponent = null;
            }
        }
        return newComponent;
    }


    /**
     * Now we have validated it is OK to add the component, insert it at the drag point
     * @param containingFlow holding the new component
     * @param ikasanComponentType to be added
     * @param x location of the drop
     * @param y location of the drop
     */
    private void insertNewComponentBetweenSurroundingPair(IkasanFlow containingFlow, IkasanComponentType ikasanComponentType, int x, int y) {
        // insert new component between surrounding pari
        Pair<IkasanFlowComponent,IkasanFlowComponent> surroundingComponents = getSurroundingComponents(x, y);
        List<IkasanFlowComponent> components = containingFlow.getFlowComponentList() ;
        int numberOfComponents = components.size();
        for (int ii = 0 ; ii < numberOfComponents ; ii++ ) {
            if (components.get(ii).equals(surroundingComponents.getRight())) {
                components.add(ii, IkasanFlowComponent.getInstance(ikasanComponentType, containingFlow));
                break;
            } else if (components.get(ii).equals(surroundingComponents.getLeft())) {
                components.add(ii+1, IkasanFlowComponent.getInstance(ikasanComponentType, containingFlow));
                break;
            }
        }
    }

    /**
     * Now we have validated it is OK to add the component, insert it at the drag point
     * @param containingFlow holding the new component
     * @param ikasanFlowComponent to be added
     * @param x location of the drop
     * @param y location of the drop
     */
    private void insertNewComponentBetweenSurroundingPair(IkasanFlow containingFlow, IkasanFlowComponent ikasanFlowComponent, int x, int y) {
        // insert new component between surrounding pari
        Pair<IkasanFlowComponent,IkasanFlowComponent> surroundingComponents = getSurroundingComponents(x, y);
        List<IkasanFlowComponent> components = containingFlow.getFlowComponentList() ;
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

    /**
     * This overrides the parent JPanel paintComponent.
     */
    @Override
    public void paintComponent(Graphics g) {
        log.debug("paintComponent invoked");
        super.paintComponent(g);
        if (initialiseAllDimensions && ikasanModule != null) {
                ikasanModule.getViewHandler().initialiseDimensions(g, 0,0, this.getWidth(), this.getHeight());
                initialiseAllDimensions = false;
        }

        if (ikasanModule != null) {
            if (ikasanModule.hasUnsetMandatoryProperties()) {
                enableStart();
            }
            ikasanModule.getViewHandler().paintComponent(this, g, -1, -1);
        } else {
            log.warn("Model not set");
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
            log.error("Error saving image to file " + file.getAbsolutePath(), ioe);
        }
    }

    public void saveAsSvg(File file, boolean useTransparentBackground) {
        DOMImplementation domImplementation = GenericDOMImplementation.getDOMImplementation();
        Document document = domImplementation.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(document);

        if (!useTransparentBackground) {
            svgGraphics2D.setColor(JBColor.WHITE);
            svgGraphics2D.fillRect(0, 0, getWidth(), getHeight());
        }
        paint(svgGraphics2D);

        try {
            svgGraphics2D.stream(file.getAbsolutePath(), true);
            StudioUIUtils.displayMessage(projectKey, "Saved file to " + file.getAbsolutePath());
        } catch (SVGGraphics2DIOException se) {
            StudioUIUtils.displayErrorMessage(projectKey, "Could not save SVG image to file " + file.getAbsolutePath());
            log.error("Error saving SVG image to file " + file.getAbsolutePath(), se);
        }
    }

    public IkasanModule getIkasanModule() {
        return ikasanModule;
    }
//    private void navigateToSource(@NotNull PsiElement classToNavigateTo, int offset)
//    {
//        PsiFile containingFile = classToNavigateTo.getContainingFile ();
//        VirtualFile virtualFile = containingFile.getVirtualFile ();
//        if (virtualFile != null)
//        {
//            FileEditorManager manager = FileEditorManager.getInstance (Context.getProject());
//            FileEditor[] fileEditors = manager.openFile (virtualFile, true);
//            if (fileEditors.length > 0)
//            {
//                FileEditor fileEditor = fileEditors [0];
//                if (fileEditor instanceof NavigatableFileEditor)
//                {
//                    NavigatableFileEditor navigatableFileEditor = (NavigatableFileEditor) fileEditor;
//                    Navigatable descriptor = new OpenFileDescriptor (Context.getProject(), virtualFile, offset);
//                    navigatableFileEditor.navigateTo (descriptor);
//                }
//            }
//        }
//    }
}
