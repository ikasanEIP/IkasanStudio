package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanPaletteElementViewHandler extends AbstractViewHandlerIntellij {
    private static final Logger LOG = Logger.getInstance("#IkasanPaletteElementViewHandler");
    private final ComponentMeta componentMeta;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param componentMeta for the view handler
     */
    public IkasanPaletteElementViewHandler(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }

    public int paintComponent(JPanel canvas, Graphics g, int topX, int topY){
        LOG.debug("STUDIO: paintComponent invoked");
        if (getDisplayIcon() != null) {
            getDisplayIcon().paintIcon(canvas, g, getLeftX(), getTopY());
        }
        return getBottomY();
    }

    @Override
    public void initialiseDimensions(Graphics graphics, int x, int y, int width, int height) {
        Thread thread = Thread.currentThread();
        LOG.warn("STUDIO: Call to initialiseDimensions on IkasanPaletteElementViewHandler not expected :" + Arrays.asList(thread.getStackTrace()));
    }

    public String getText() {
        return componentMeta.getName();
    }
    public String getHelpText() {
        return componentMeta.getHelpText();
    }

    public ImageIcon getDisplayIcon() {
        return componentMeta.getSmallIcon();
    }

    @Override
    public String toString() {
        return "IkasanPaletteElementViewHandler{" +
                "componentMeta=" + componentMeta +
                '}';
    }

    /**
     * Perform any tidy up during deletion of this element
     */
    @Override
    public void dispose() {

    }
}
