package org.ikasan.studio.ui.model;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;

public class IkasanFlowUIComponentTransferable implements Serializable, Transferable {
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(IkasanFlowUIComponent.class, "IkasanFlowUIComponent");
    private static final DataFlavor flavors[] = { ikasanFlowUIComponentFlavor };
    IkasanFlowUIComponent ikasanFlowUIComponent ;

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

//    @Override
//    public DataFlavor[] getTransferDataFlavors() {
//        return new DataFlavor[0];
//    }
//
//    @Override
//    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
//        return false;
//    }
//
//    @NotNull
//    @Override
//    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
//        return null;
//    }
}
