package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentTypeMeta;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentTransferable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class CanvasImportTransferHandler extends TransferHandler // implements Transferable
{
    private static final Logger LOG = Logger.getInstance("#CanvasImportTransferHandler");
    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(IkasanFlowUIComponent.class, "IkasanFlowUIComponent");
    private final DesignerCanvas designerCanvas;

    public CanvasImportTransferHandler(DesignerCanvas designerCanvas) {
        this.designerCanvas = designerCanvas;
    }

    /**
     * This method is called repeatedly during a drag and drop operation to allow the developer to configure properties of, and to return
     * the acceptability of transfers; with a return value of {@code true} indicating that the transfer represented by the given
     * {@code TransferSupport} (which contains all of the details of the transfer) is acceptable at the current time, and a value of {@code false}
     * rejecting the transfer.
     * <p>
     * For those components that automatically display a drop location during drag and drop, accepting the transfer, by default, tells them to show
     * the drop location. This can be changed by calling {@code setShowDropLocation} on the {@code TransferSupport}.
     * <p>
     * By default, when the transfer is accepted, the chosen drop action is that picked by the user via their drag gesture. The developer can override
     * this and choose a different action, from the supported source actions, by calling {@code setDropAction} on the {@code TransferSupport}.
     * <p>
     * On every call to {@code canImport}, the {@code TransferSupport} contains  fresh state. As such, any properties set on it must be set on every
     * call. Upon a drop, {@code canImport} is called one final time before  calling into {@code importData}. Any state set on the
     * {@code TransferSupport} during that last call will be available in {@code importData}.
     * <p>
     * This method is not called internally in response to paste operations. As such, it is recommended that implementations of {@code importData}
     * explicitly call this method for such cases and that this method be prepared to return the suitability of paste operations as well.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of the transfer, not <code>null</code>.
     * @return <code>true</code> if the import can happen, <code>false</code> otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    @Override
    public boolean canImport(final TransferHandler.TransferSupport support) {
        final Component targetComponent = support.getComponent();
        final DataFlavor[] destinationSupportedflavors = support.getDataFlavors();

        LOG.trace("Can import check " + targetComponent);
        // Only allow drop (not paste) and ignore unless on canvas
        if (! support.isDrop() ||
            // and only on the canvas
            !(targetComponent instanceof DesignerCanvas)) {
            return false;
        }

        // Since the canvas is not a simple widget with built in drop handlers, we need to perform that ourselves.
        if (!designerCanvas.getIkasanModule().hasUnsetMandatoryProperties()) {
            for(DataFlavor flavor : destinationSupportedflavors) {
                // Anywhere on the canvas
                if (flavor.equals(ikasanFlowUIComponentFlavor)) {
                    return flowInFocusActions(support);
                }
            }
        }

        return false;
    }

    /**
     * Determine if there is a flow beneath the mouse and if so, cause appropriate highlighting
     * @param support is the specialised class that encapsulates the clipboard.
     * @return true if the mouse is currently over a flow.
     */
    private boolean flowInFocusActions(final TransferHandler.TransferSupport support) {
        boolean mouseOverFlow = false;
        Point currentMouse = support.getDropLocation().getDropPoint();
        IkasanFlowUIComponent ikasanFlowUIComponent = getDraggedComponent(support);
        if ((ikasanFlowUIComponent != null && ikasanFlowUIComponent.getIkasanComponentType() != null && IkasanComponentTypeMeta.FLOW.equals(ikasanFlowUIComponent.getIkasanComponentType())) ||
                designerCanvas.isFlowAtXY(currentMouse.x, currentMouse.y)) {
            mouseOverFlow = true;
        }
        designerCanvas.componentDraggedToFlowAction(currentMouse.x, currentMouse.y, ikasanFlowUIComponent);
        return mouseOverFlow;
    }
    /**
     * Causes a transfer to occur from a clipboard or a drag and drop operation. The <code>Transferable</code> to be
     * imported and the component to transfer to are contained within the <code>TransferSupport</code>.
     * <p>
     * While the drag and drop implementation calls {@code canImport} to determine the suitability of a transfer before calling this
     * method, the implementation of paste does not. As such, it cannot be assumed that the transfer is acceptable upon a call to
     * this method for paste. It is recommended that {@code canImport} be explicitly called to cover this case.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of the transfer, not <code>null</code>.
     * @return true if the data was inserted into the component, false otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    @Override
    public boolean importData(TransferSupport support) {
        LOG.info("import Data invoked " + support);
        if (this.canImport(support)) {
            IkasanFlowUIComponent ikasanFlowUIComponent = getDraggedComponent(support);
            if (ikasanFlowUIComponent != null) {
                IkasanComponentTypeMeta ikasanComponentType = ikasanFlowUIComponent.getIkasanComponentType();
                return designerCanvas.requestToAddComponent(support.getDropLocation().getDropPoint().x, support.getDropLocation().getDropPoint().y, ikasanComponentType);
            }
        }
        return false;
    }

    /**
     * Extract the ikasan component being dragged from the Transfer support object
     * @param support standard object containing information about the dragged component.
     * @return the instance of the IkasanFlowUIComponent currently being dragged.
     */
    private IkasanFlowUIComponent getDraggedComponent(TransferSupport support) {
        IkasanFlowUIComponent ikasanFlowComponent = null;
        try {
            IkasanFlowUIComponentTransferable  ikasanFlowUIComponentTransferable = (IkasanFlowUIComponentTransferable)support.getTransferable().getTransferData(ikasanFlowUIComponentFlavor);
            ikasanFlowComponent = ikasanFlowUIComponentTransferable.getIkasanFlowUIComponent();
        } catch (IOException | UnsupportedFlavorException e) {
            LOG.warn("Could not import flavor " + ikasanFlowUIComponentFlavor + " from support " + support + " due to exception " + e.getMessage());
            e.printStackTrace();
        }
        return ikasanFlowComponent;
    }
}

