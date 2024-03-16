package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import lombok.Data;
import org.ikasan.studio.build.model.ikasan.instance.ViewHandler;

import javax.swing.*;
import java.awt.*;

@Data
public abstract class AbstractViewHandler implements ViewHandler {
    private static final Logger LOG = Logger.getInstance("#AbstractViewHandler");
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
    public abstract int paintComponent(JPanel canvas, Graphics g, int minimumTopX, int minimumTopY);

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
    public void initialiseDimensions(Graphics graphics, int x, int y, int width, int height) {}

    /**
     * Get the y position for the bottom of the component
     * @return y position for the bottom of the component
     */
    public int getBottomY() {
        return getTopY() + getHeight();
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
}