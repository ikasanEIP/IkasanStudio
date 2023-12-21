package org.ikasan.studio.ui.component.palette;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentTransferable;
import org.ikasan.studio.ui.model.PaletteItemIkasanComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class PaletteExportTransferHandler extends TransferHandler // implements Transferable
{
    private static final Logger LOG = Logger.getInstance("#PaletteExportTransferHandler");
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(IkasanFlowUIComponent.class, "IkasanFlowUIComponent");
    private static final DataFlavor flavors[] = { ikasanFlowUIComponentFlavor };

    // Source actions i.e. methods called for the source of the copy

    /**
     * For PaletteExportTransferHandler we only want to copy a component onto the design window, not link or move
     * @param sourceComponent the sourceComponent
     * @return
     */
    @Override
    public int getSourceActions(JComponent sourceComponent) {
        return TransferHandler.COPY;
    }

    @Override
    public Transferable createTransferable(JComponent sourceComponent) {
        if (sourceComponent instanceof JList && ((JList)sourceComponent).getSelectedValue() instanceof PaletteItemIkasanComponent) {
            JList paletteList = (JList)sourceComponent;
            PaletteItemIkasanComponent item = (PaletteItemIkasanComponent)paletteList.getSelectedValue();
            IkasanFlowUIComponentTransferable newTranferrable = new IkasanFlowUIComponentTransferable(item.getIkasanFlowUIComponent());
            Image dragImage = item.getIkasanFlowUIComponent().getSmallIcon().getImage();
            if (dragImage != null) {
                setDragImage(dragImage);
            }

            return newTranferrable;
        }

        return null;
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
    @Override
    public boolean canImport(JComponent targetComponent, DataFlavor destinationSupportedflavors[]) {
//        log.trace("can Import check " + targetComponent);
//        if (!(targetComponent instanceof DesignerCanvas)) {
//            log.trace("exit due to wrong component " + targetComponent);
//            return false;
//        }
//
//        System.out.println("On the canvas");
//        for(DataFlavor flavor : destinationSupportedflavors) {
//            if (flavor.equals(ikasanFlowUIComponentFlavor)) {
//                // OK seems we maybe can drop here or can we
//                return true;
//            }
//        }
        // Cant impot anything to the Pallette
        return false;
    }
// maybe the other would mean we could have different classes from source and destination, not sure.
//public boolean canImport(TransferHandler.TransferSupport support) {
//    return support.getComponent() instanceof JComponent ? this.canImport((JComponent)support.getComponent(), support.getDataFlavors()) : false;
//}

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        LOG.info("Got " + support);
        return super.importData(support);

    }

    /**
     * This method is called on a successful drop (or paste) and initiates the transfer of data to the target component.
     * This method returns true if the import was successful and false otherwise.
     * @param targetComponent under the mouse that has registered as being able to recieve this flavor of component.
     * @param t the data object being dragged.
     * @return
     */
    @Override
    public boolean importData(JComponent targetComponent, Transferable t) {

        if (targetComponent instanceof JPanel) {
            JPanel jpanel = (JPanel) targetComponent;
            if (t.isDataFlavorSupported(ikasanFlowUIComponentFlavor)) {
                try {
                    IkasanFlowUIComponentTransferable ikasanFlowUIComponent = (IkasanFlowUIComponentTransferable) t.getTransferData(ikasanFlowUIComponentFlavor);
                    LOG.warn("I can perhaps drop " + ikasanFlowUIComponent);

                    return true;
                } catch (UnsupportedFlavorException | IOException ignored) {
                }
            }
        }
        return false;
    }

}

