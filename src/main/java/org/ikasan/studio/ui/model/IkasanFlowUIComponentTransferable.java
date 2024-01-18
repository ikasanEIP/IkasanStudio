package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.ikasan.instance.FlowElement;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;

/**
 * This class holds the data that is being dragged by the mouse.
 */
public class IkasanFlowUIComponentTransferable implements Serializable, Transferable {
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(FlowElement.class, "IkasanFlowUIComponent");
    private static final DataFlavor[] flavors = { ikasanFlowUIComponentFlavor };
    private FlowElement flowElement ;

    public IkasanFlowUIComponentTransferable(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    // Transferable
    public Object getTransferData(DataFlavor flavor) {
        if (isDataFlavorSupported(flavor)) {
            return this;
        }
        return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ikasanFlowUIComponentFlavor.equals(flavor);
    }
    public FlowElement getFlowElement() {
        return flowElement;
    }

}
