package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.core.model.ikasan.instance.AbstractViewHandler;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Abstract class for view handlers residing in the UI package. Its parent resides in the core package in order to bridge
 * the gap between the core model and the UI representation of that model.
 * Provides methods for painting components, managing dimensions, and handling navigation.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public abstract class AbstractViewHandlerIntellij extends AbstractViewHandler {
    private static final Logger LOG = Logger.getInstance("#AbstractViewHandlerIntellij");
    PsiClass classToNavigateTo;
    PsiFile psiFile;
    int offsetInclassToNavigateTo;
    private int topY;
    private int leftX;
    private int width;
    private int height;
    private int trailingGap;
    private int leadingGap;
    private int minimumGap;
    boolean isAlreadySelected;  // Determines if a component is already selected e.g. to prevent a reclick from resetting edit changes.

    /**
     * Paint the components belonging to the view handler
     * @param canvas panel to paint on
     * @param g Swing graphics class
     * @param minimumTopY top y of the component, sometimes we need to supply this, otherwise -1 will allow viewHandler to
     *             determine
     * @return the bottom Y value for this component, sometimes we need this to pass onto the next
     */
    public abstract int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY) ;

    public Point getLeftConnectorPoint() {
        return new Point(getLeftX(), getTopY() + (getHeight()/2));
    }
    public Point getRightConnectorPoint() {
        return new Point(getRightX(), getTopY() + (getHeight()/2));
    }
    public Point getTopConnectorPoint() {
        return new Point(getLeftX() + (getWidth()/2), getTopY());
    }
    public Point getBottomConnectorPoint() {
        return new Point(getLeftX() + (getWidth()/2), getTopY() + getHeight());
    }
    public Point getCentrePoint()  {
        return new Point(getLeftX() + (getWidth()/2), getTopY() + (getHeight()/2));
    }

    protected int getMinimumGap() {
        return UiContext.getMinimumComponentXSpacing();
    }
    /**
     *
     * @param graphics object
     * @param x starting point
     * @param y starting point
     * @param width of container
     * @param height of container
     */
    public abstract void initialiseDimensions(Graphics graphics, int x, int y, int width, int height) ;

    /**
     * Get the y position for the bottom of the component
     * @return y position for the bottom of the component
     */
    public int getBottomY() {
        return getTopY() + getHeight();
    }
    /**
     * Get the y position for the bottom of the component
     * @return y position for the bottom of the component
     */
    public int getBottomYPlusText(Graphics g) {
        return getTopY() + getHeight() + getTextHeight(g);
    }

    /**
     * Get the TestV1 position of the right hand side of the component
     * @return TestV1 position of the right hand side of the component
     */
    public int getRightX() {
        return leftX + width;
    }

    public void setLeftX(int leftX) {
        if (leftX < -10) {
            LOG.error("STUDIO: Left X being set to a -ve of " + leftX);
        }
        this.leftX = leftX;
    }

    public void setWidth(int width) {
        if (width < -10) {
            LOG.error("STUDIO: width being set to a -ve of " + width);
        }

        this.width = width;
    }

    public void setHeight(int height) {
        if (height < 0) {
            LOG.error("STUDIO: Height less than 0") ;
        }
        this.height = height;
    }

    /**
     * Return true if this component is at the X and Y coordinate
     * Remember 0,0 is top, left i.e. Y increases downwards
     * @param x in question
     * @param y in question
     * @return true if this component (or its wiretaps) are at that location
     */
    public boolean isComponentAtXY(int x, int y) {
        return getLeftX() <= x && x <= getRightX() &&
               getTopY()  <= y && y <= getBottomY();
    }

    public abstract String getText();


    /**
     * Perform any tidy up during deletion of this element
     */
    public abstract void dispose();

    protected int getTextWidth(Graphics g) {
        return StudioUIUtils.getTextWidth(g, getText());
    }

    protected int getTextHeight(Graphics g) {
        return StudioUIUtils.getTextHeight(g);
    }

    public boolean isAlreadySelected() {
        return isAlreadySelected;
    }

    public void setAlreadySelected(boolean alreadySelected) {
        isAlreadySelected = alreadySelected;
    }

    public void setPsiFile(PsiFile psiFile) {
        this.psiFile = psiFile;
        if (psiFile instanceof PsiJavaFile) {
            PsiClass[] allClasses = ((PsiJavaFile)psiFile).getClasses();
            if (allClasses.length > 0) {
                // for now, assume the main class is the first in the array
                classToNavigateTo = allClasses[0];
            }
        }
    }

    public int getLeadingGap() {
        return Math.max(getMinimumGap(), leadingGap);
    }

    public int getTrailingGap() {
        return Math.max(getMinimumGap(), trailingGap);
    }

    /**
     * If the view handler exists for the BasicElement, return it, otherwise get a new one and set it on the BasicElement
     * @param project is the Intellij project instance
     * @param ikasanBasicElement to be examined
     * @return the view handler for this element.
     */
    public AbstractViewHandlerIntellij getOrCreateAbstractViewHandler(Project project, BasicElement ikasanBasicElement) {
        AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement);
        return verifyHandler(viewHandler);
    }

    public IkasanFlowComponentViewHandler getOrCreateFlowComponentViewHandler(Project project, BasicElement ikasanBasicElement) {
        IkasanFlowComponentViewHandler viewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, ikasanBasicElement);
        return (IkasanFlowComponentViewHandler)verifyHandler(viewHandler);
    }

    public IkasanFlowViewHandler getOrCreateFlowViewViewHandler(Project project, Flow flow) {
        IkasanFlowViewHandler viewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
        return (IkasanFlowViewHandler)verifyHandler(viewHandler);
    }

    protected AbstractViewHandlerIntellij verifyHandler(AbstractViewHandlerIntellij viewHandler) {
        if (viewHandler.equals(this)) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: SERIOUS: call for a view handler returned this view handler, this must be a mistake, ignoring" + Arrays.toString(thread.getStackTrace()));
            viewHandler = null;
        }
        return viewHandler;
    }

    /**
     * Use the value or reset if it is greater than -1
     * @param reset set to -1 if we don't yet know if we need to override the current value
     * @param current value that might be overriden
     * @return reset unless it was -1
     */
    protected int checkForReset(int reset, int current) {
        if (reset > -1) {
            return reset;
        } else {
            return current;
        }
    }
}
