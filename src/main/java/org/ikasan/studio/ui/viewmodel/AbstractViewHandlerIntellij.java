package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.core.model.ikasan.instance.AbstractViewHandler;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper=true)
public abstract class AbstractViewHandlerIntellij extends AbstractViewHandler {
    private static final Logger LOG = Logger.getInstance("#AbstractViewHandlerIntellij");
    PsiClass classToNavigateTo;
    PsiJavaFile psiJavaFile;
    int offsetInclassToNavigateTo;
    private int topY;
    private int leftX;
    private int width;
    private int height;
    boolean isAlreadySelected;

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
     * Get the x position of the right hand side of the component
     * @return x position of the right hand side of the component
     */
    public int getRightX() {
        return leftX + width;
    }

    public void setLeftX(int leftX) {
        if (leftX < -10) {
            LOG.error("Left X being set to a -ve of " + leftX);
        }
        this.leftX = leftX;
    }

    public void setWidth(int width) {
        if (width < -10) {
            LOG.error("Left X being set to a -ve of " + width);
        }

        this.width = width;
    }

    public void setHeight(int height) {
        if (height < 0) {
            LOG.error("Height less than 0") ;
        }
        this.height = height;
    }

    public abstract String getText();

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

    public void setPsiJavaFile(PsiJavaFile psiJavaFile) {
        this.psiJavaFile = psiJavaFile;
        if (psiJavaFile != null) {
            PsiClass[] allClasses = psiJavaFile.getClasses();
            if (allClasses.length > 0) {
                // for now, assume the main class is the first in the array
                classToNavigateTo = allClasses[0];
            }
        }
    }

    public AbstractViewHandler getViewHandler(String projectKey) {
        return null;
    }

    public void setViewHandler(String projectKey) {
    }

    /**
     * If the view handler exists for the BasicElement, return it, otherwise get a new one and set it on the BasicElement
     * @param projectKey to provide scope
     * @param ikasanBasicElement to be examined
     * @return the view handler for this element.
     */
    public AbstractViewHandlerIntellij getOrCreateAbstractViewHandler(String projectKey, BasicElement ikasanBasicElement) {
        AbstractViewHandlerIntellij viewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, ikasanBasicElement);
        return verifyHandler(viewHandler);
    }

    public IkasanFlowComponentViewHandler getOrCreateFlowComponentViewHandler(String projectKey, BasicElement ikasanBasicElement) {
        IkasanFlowComponentViewHandler viewHandler = ViewHandlerFactoryIntellij.getOrCreateFlowComponentViewHandler(projectKey, ikasanBasicElement);
        return (IkasanFlowComponentViewHandler)verifyHandler(viewHandler);
    }

    public IkasanFlowViewHandler getOrCreateFlowViewViewHandler(String projectKey, Flow flow) {
        IkasanFlowViewHandler viewHandler = ViewHandlerFactoryIntellij.getFlowViewHandler(projectKey, flow);
        return (IkasanFlowViewHandler)verifyHandler(viewHandler);
    }

    protected AbstractViewHandlerIntellij verifyHandler(AbstractViewHandlerIntellij viewHandler) {
        if (viewHandler.equals(this)) {
            Thread thread = Thread.currentThread();
            LOG.warn("DANGER: call for a view handler returned this view handler, this must be a mistake, ignoring" + Arrays.toString(thread.getStackTrace()));
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
