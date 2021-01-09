package org.ikasan.studio.ui.viewmodel;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import javax.swing.*;
import java.awt.*;

public abstract class ViewHandler {
    PsiClass classToNavigateTo;
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

    public abstract void initialiseDimensions(Graphics g, int x, int y, int width, int height) ;

    /**
     * Get the y position for the bottom of the component
     * @return
     */
    public int getBottomY() {
        return getTopY() + getHeight();
    }

    /**
     * Get the x position of the right hand side of the component
     * @return
     */
    public int getRightX() {
        return leftX + width;
    }

    public int getTopY() {
        return topY;
    }

    public void setTopY(int topY) {
        this.topY = topY;
    }

    public int getLeftX() {
        return leftX;
    }

    public void setLeftX(int leftX) {
        this.leftX = leftX;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isAlreadySelected() {
        return isAlreadySelected;
    }

    public void setAlreadySelected(boolean alreadySelected) {
        isAlreadySelected = alreadySelected;
    }

    public void setClassToNavigateTo(PsiClass classToNavigateTo) {
        this.classToNavigateTo = classToNavigateTo;
    }

    public PsiClass getClassToNavigateTo() {
        return classToNavigateTo;
    }

    public int getOffsetInclassToNavigateTo() {
        return offsetInclassToNavigateTo;
    }

    public void setOffsetInclassToNavigateTo(int offsetInclassToNavigateTo) {
        this.offsetInclassToNavigateTo = offsetInclassToNavigateTo;
    }
}
