package org.ikasan.studio.ui.viewmodel;

import org.apache.log4j.Logger;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanPaletteElementViewHandler extends ViewHandler {
    private static final Logger log = Logger.getLogger(IkasanPaletteElementViewHandler.class);
    IkasanFlowUIComponent model;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param model
     */
    public IkasanPaletteElementViewHandler(IkasanFlowUIComponent model) {
        this.model = model;
    }

    public int paintComponent(JPanel canvas, Graphics g, int topX, int topY){
        log.debug("paintComponent invoked");
        if (getDisplayIcon() != null) {
            getDisplayIcon().paintIcon(canvas, g, getLeftX(), getTopY());
        }
        return getBottomY();
    }

    public String getText() {
        return model.getTitle();
    }
    public String getHelpText() {
        return model.getHelpText();
    }

    public ImageIcon getDisplayIcon() {
        return model.getSmallIcon();
    }

    @Override
    public String toString() {
        return "IkasanPaletteElementViewHandler{" +
                "model=" + model +
                '}';
    }
}
