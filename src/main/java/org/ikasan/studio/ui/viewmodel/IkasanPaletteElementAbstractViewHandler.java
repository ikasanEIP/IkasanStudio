package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.build.model.ikasan.meta.ComponentMeta;

import javax.swing.*;
import java.awt.*;

/**
 * Abstracts away UI details and provides access to appropriate presentation state from the domain model
 */
public class IkasanPaletteElementAbstractViewHandler extends AbstractViewHandler {
    private static final Logger LOG = Logger.getInstance("#IkasanPaletteElementAbstractViewHandler");
    private final ComponentMeta componentMeta;

    /**
     * The model can be null e.g. for a palette item, once dragged onto a canvas, the model would be populated.
     * @param componentMeta for the view handler
     */
    public IkasanPaletteElementAbstractViewHandler(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
    }

    public int paintComponent(JPanel canvas, Graphics g, int topX, int topY){
        LOG.debug("paintComponent invoked");
        if (getDisplayIcon() != null) {
            getDisplayIcon().paintIcon(canvas, g, getLeftX(), getTopY());
        }
        return getBottomY();
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
        return "IkasanPaletteElementAbstractViewHandler{" +
                "componentMeta=" + componentMeta +
                '}';
    }
}
