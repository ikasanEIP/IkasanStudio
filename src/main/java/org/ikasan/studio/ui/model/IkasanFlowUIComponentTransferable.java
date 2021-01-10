package org.ikasan.studio.ui.model;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;

/**
 * This class holds the data that is being dragged by the mouse.
 */
public class IkasanFlowUIComponentTransferable implements Serializable, Transferable {
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(IkasanFlowUIComponent.class, "IkasanFlowUIComponent");
    private static final DataFlavor flavors[] = { ikasanFlowUIComponentFlavor };
    private IkasanFlowUIComponent ikasanFlowUIComponent ;

    public IkasanFlowUIComponentTransferable(IkasanFlowUIComponent ikasanFlowUIComponent) {
        this.ikasanFlowUIComponent = ikasanFlowUIComponent;
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
    public IkasanFlowUIComponent getIkasanFlowUIComponent() {
        return ikasanFlowUIComponent;
    }

}
