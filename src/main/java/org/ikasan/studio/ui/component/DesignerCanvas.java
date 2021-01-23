package org.ikasan.studio.ui.component;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.Navigator;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowElement;
import org.ikasan.studio.model.Ikasan.IkasanFlowElementType;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.SUIUtils;
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

public class DesignerCanvas extends JPanel {
    private boolean initialiseCanvas = true;
    private boolean drawGrid = false;
    private static final Logger log = Logger.getLogger(DesignerCanvas.class);
    IkasanModule ikasanModule ;
    private int clickStartMouseX = 0 ;
    private int clickStartMouseY = 0 ;
    private boolean screenChanged = false;
    private String projectKey ;

    public DesignerCanvas(String projectKey) {
        this.projectKey = projectKey;
        ikasanModule = Context.getIkasanModule(projectKey);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setBackground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mouseClick(e, e.getX(),e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                mouseRelease(e, e.getX(),e.getY());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                moveComponent(e, e.getX(),e.getY());
            }
        });

        setTransferHandler(new UIComponentImportTransferHandler( this));
    }

    /**
     * This is called by the mouse click Listener
     * @param me mouse event
     * @param x of the start of the mouse click
     * @param y of the start of the mouse click
     */
    private void mouseClick(MouseEvent me, int x, int y) {
        clickStartMouseX = x;
        clickStartMouseY = y;
        IkasanFlowElement mouseSelectedComponent = getComponentAtXY(x, y);
        log.info("Mouse mouseClick x [" + clickStartMouseX + "] y [" + clickStartMouseY + "] selected component [" + mouseSelectedComponent + "]");

          // One click, show popup
        if (me.getButton() == MouseEvent.BUTTON3) {
            if (mouseSelectedComponent != null) {
                DesignCanvasContextMenu.showPopupAndNavigateMenu(projectKey, this, me, mouseSelectedComponent);
            } else {
                DesignCanvasContextMenu.showPopupMenu(projectKey,this, me);
            }
        } // Double click -> go to source
        else if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2 && ! me.isConsumed() && mouseSelectedComponent != null) {
            me.consume();
            if (mouseSelectedComponent.getViewHandler().getOffsetInclassToNavigateTo() != 0) {
                Navigator.navigateToSource(projectKey, mouseSelectedComponent.getViewHandler().getClassToNavigateTo(), mouseSelectedComponent.getViewHandler().getOffsetInclassToNavigateTo());
            } else {
                Navigator.navigateToSource(projectKey, mouseSelectedComponent.getViewHandler().getClassToNavigateTo());
            }
        } // Single click -> update properties
        else if (me.getButton() == MouseEvent.BUTTON1 && mouseSelectedComponent != null) {
            if (!mouseSelectedComponent.getViewHandler().isAlreadySelected()) {
                setSelectedComponent(mouseSelectedComponent);
                Context.getPropertiesPanel(projectKey).updatePropertiesPanel(mouseSelectedComponent);
            }
        }
    }

    /**
     * This will be called to redraw the screen after the mouse movement is over, the mouse movement will allow the
     * component to move across the screen but not redraw the connectors, this will ensure all connectors and whole
     * screen is redrawn
     * @param me mouse event
     * @param x of the current pointer
     * @param y of the current pointer
     */
    private void mouseRelease(MouseEvent me, int x, int y){
        if (screenChanged) {
            this.repaint();
            screenChanged = false;
        }
    }

    /**
     * Called by the mouse drag listener
     * @param me of the event
     * @param mouseX at the start of the drag
     * @param mouseY at the start of the drag
     */
    private void moveComponent(MouseEvent me, int mouseX, int mouseY){
        boolean didMouseClickStartOnComponents = (getComponentAtXY(clickStartMouseX, clickStartMouseY) != null);
        IkasanFlowElement mouseSelectedComponent = getComponentAtXY(mouseX, mouseY);
        if (didMouseClickStartOnComponents && mouseSelectedComponent != null) {
            screenChanged = true;
            ViewHandler vh = mouseSelectedComponent.getViewHandler();
            log.info("Mouse drag start x[ " + clickStartMouseX + "] y " + clickStartMouseY + "] now  x [" + mouseX + "] y [" + mouseY +
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

    public void setSelectedComponent(IkasanFlowElement flowComponent) {
        ikasanModule.getFlows()
                .stream()
                .flatMap(x -> x.getFlowElementList().stream())
                .filter(x -> x.getViewHandler().isAlreadySelected())
                .peek(x -> x.getViewHandler().setAlreadySelected(false));
        ikasanModule.getFlows()
                .stream()
                .flatMap(x -> x.getFlowElementList().stream())
                .filter(x -> x.equals(flowComponent))
                .peek(x -> x.getViewHandler().setAlreadySelected(true));
    }

    /**
     * Given the x and y coords, return the Ikasan elements that resides at that x,y.
     * @param xpos
     * @param ypos
     * @return
     */
    public IkasanFlowElement getComponentAtXY(int xpos, int ypos) {
        IkasanFlowElement ikasanFlow = null;
        if (ikasanModule != null) {
            ikasanFlow = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowElementList().stream())
                    .filter(x -> x.getViewHandler().getLeftX() <= xpos && x.getViewHandler().getRightX() >= xpos && x.getViewHandler().getTopY() <= ypos && x.getViewHandler().getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
        }
        return ikasanFlow;
    }

    public boolean requestToAddComponent(int x, int y, IkasanFlowElementType ikasanFlowElementType) {
        if (x >= 0 && y >=0 && ikasanFlowElementType != null) {
//            if (ikasanModule.getFlows().isEmpty()) {
                // drop here and create a new flow.
                // or create source and regenerate ikasanModule ??
                IkasanFlow newFlow = new IkasanFlow();
                ikasanModule.addAnonymousFlow(newFlow);
                newFlow.addFlowElement(new IkasanFlowElement(ikasanFlowElementType, newFlow));

                PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
                pipsiIkasanModel.generateSourceFromModule();
                StudioPsiUtils.resetIkasanModuleFromSourceCode(projectKey, false);
                initialiseCanvas = true;
//            } else {
//                // add the components to the flow.
//            }
            this.repaint();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Given the x and y coords, return Ikasan elements to the left or right (or both) within reasonable bounds
     * @param xpos
     * @param ypos
     * @return
     */
    public IkasanFlowElement getComponentsSurroundingY(int xpos, int ypos) {
        IkasanFlowElement ikasanFlow = null;
        if (ikasanModule != null) {
            ikasanFlow = ikasanModule.getFlows()
                    .stream()
                    .flatMap(x -> x.getFlowElementList().stream())
                    .filter(x -> x.getViewHandler().getLeftX() <= xpos && x.getViewHandler().getRightX() >= xpos && x.getViewHandler().getTopY() <= ypos && x.getViewHandler().getBottomY() >= ypos)
                    .findFirst()
                    .orElse(null);
        }
        return ikasanFlow;
    }

    /**
     * This overrides the parent JPanel paintComponent.
     */
    @Override
    public void paintComponent(Graphics g) {
        log.debug("paintComponent invoked");
        super.paintComponent(g);
        if (initialiseCanvas && ikasanModule != null) {
                ikasanModule.getViewHandler().initialiseDimensions(g, 0,0, this.getWidth(), this.getHeight());
                initialiseCanvas = false;
        }
        if (ikasanModule != null) {
            ikasanModule.getViewHandler().paintComponent(this, g, -1, -1);
        }
        if (drawGrid) {
            SUIUtils.paintGrid(g, 0, 0, getWidth(), getHeight());
        }
    }

    public void setInitialiseCanvas(boolean initialiseCanvas) {
        this.initialiseCanvas = initialiseCanvas;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    public void saveAsImage(File file, String imageFormat, boolean transparentBackground) {
        int imageType = transparentBackground ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage bufferedImage = UIUtil.createImage(getWidth(), getHeight(), imageType);
        Graphics graphics = bufferedImage.getGraphics();
        if (!transparentBackground) {
            graphics.setColor(JBColor.WHITE);
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        paint(graphics);
        try {
            boolean saved = ImageIO.write(bufferedImage, imageFormat, file);
            if (!saved) {
                SUIUtils.displayErrorMessage(projectKey, "Could not save file " + file.getAbsolutePath());
            } else {
                SUIUtils.displayMessage(projectKey, "Saved file to " + file.getAbsolutePath());
            }
        } catch (IOException ioe) {
            SUIUtils.displayErrorMessage(projectKey, "Could not save image to file " + file.getAbsolutePath());
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
            SUIUtils.displayMessage(projectKey, "Saved file to " + file.getAbsolutePath());
        } catch (SVGGraphics2DIOException se) {
            SUIUtils.displayErrorMessage(projectKey, "Could not save SVG image to file " + file.getAbsolutePath());
            log.error("Error saving SVG image to file " + file.getAbsolutePath(), se);
        }
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
