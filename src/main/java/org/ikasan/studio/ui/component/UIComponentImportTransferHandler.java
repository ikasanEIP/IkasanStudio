package org.ikasan.studio.ui.component;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanFlowElementType;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentTransferable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class UIComponentImportTransferHandler extends TransferHandler // implements Transferable
{
    private static final Logger log = Logger.getLogger(UIComponentImportTransferHandler.class);
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(IkasanFlowUIComponent.class, "IkasanFlowUIComponent");
    private static final DataFlavor flavors[] = { ikasanFlowUIComponentFlavor };
    private String projectKey;
    private DesignerCanvas designerCanvas;

    public UIComponentImportTransferHandler(String projectKey, DesignerCanvas designerCanvas) {
        this.projectKey = projectKey;
        this.designerCanvas = designerCanvas;
    }

    //These methods are invoked for the drop gesture, or the paste action, when the component is the target of the operation

    /**
     * This method is called repeatedly during a drag gesture and returns true if the area below the cursor can accept the
     * transfer, or false if the transfer will be rejected. For example, if a user drags a color over a component that
     * accepts only text, the canImport method for that component's TransferHandler should return false.
     * @param targetComponent that the mouse is over that has registered this as its Transfer Handler.
     * @param destinationSupportedflavors
     * @return
     */
    public boolean canImport(JComponent targetComponent, DataFlavor destinationSupportedflavors[]) {
        log.trace("Component is " + targetComponent);
        if (!(targetComponent instanceof JPanel)) {
            return false;
        }

        for(DataFlavor flavor : destinationSupportedflavors) {
            if (flavor.equals(ikasanFlowUIComponentFlavor)) {
                return true;
            }
        }

        return false;
    }

    public boolean importData(TransferSupport support) {
        if (this.canImport((JComponent)support.getComponent(),support.getDataFlavors())) {
            IkasanFlowElementType ikasanFlowElementType = null;
            try {
                IkasanFlowUIComponentTransferable  ikasanFlowUIComponentTransferable = (IkasanFlowUIComponentTransferable)support.getTransferable().getTransferData(ikasanFlowUIComponentFlavor);
                ikasanFlowElementType = ikasanFlowUIComponentTransferable.getIkasanFlowUIComponent().getIkasanFlowElementType();
            } catch (IOException | UnsupportedFlavorException e) {
                log.error("Could not import flavor " + ikasanFlowUIComponentFlavor + " from support " + support + " due to exception " + e.getMessage());
                e.printStackTrace();
            }
            return designerCanvas.requestToAddComponent(support.getDropLocation().getDropPoint().x, support.getDropLocation().getDropPoint().y, ikasanFlowElementType);
        } else {
            return false;
        }
    }
}

