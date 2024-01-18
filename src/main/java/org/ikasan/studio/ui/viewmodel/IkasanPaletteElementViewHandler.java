package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.model.ikasan.instance.FlowElement;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanPaletteElementViewHandler extends ViewHandler {
    private static final Logger LOG = Logger.getInstance("#IkasanPaletteElementViewHandler");
    FlowElement flowElement;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param flowElement for the view handler
     */
    public IkasanPaletteElementViewHandler(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    public int paintComponent(JPanel canvas, Graphics g, int topX, int topY){
        LOG.debug("paintComponent invoked");
        if (getDisplayIcon() != null) {
            getDisplayIcon().paintIcon(canvas, g, getLeftX(), getTopY());
        }
        return getBottomY();
    }

    public String getText() {
        return flowElement.getIkasanComponentMeta().getName();
    }
    public String getHelpText() {
        return flowElement.getIkasanComponentMeta().getHelpText();
    }

    public ImageIcon getDisplayIcon() {
        return flowElement.getIkasanComponentMeta().getSmallIcon();
    }

    @Override
    public String toString() {
        return "IkasanPaletteElementViewHandler{" +
                "model=" + flowElement +
                '}';
    }
}
