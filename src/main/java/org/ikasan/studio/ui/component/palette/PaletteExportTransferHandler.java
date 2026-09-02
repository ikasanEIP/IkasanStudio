package org.ikasan.studio.ui.component.palette;

import org.ikasan.studio.ui.icons.ComponentIconProvider;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentTransferable;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

public class PaletteExportTransferHandler extends TransferHandler // implements Transferable
{
    private static final Logger LOG = Logger.getInstance("#PaletteExportTransferHandler");
//    private static final DataFlavor ikasanFlowUIComponentFlavor = new DataFlavor(FlowElement.class, "FlowElement");
//    private static final DataFlavor[] flavors = { ikasanFlowUIComponentFlavor };
    private final Project project;

    // Source actions i.e. methods called for the source of the copy

    public PaletteExportTransferHandler(Project project) {
        this.project = project;
    }

    /**
     * For PaletteExportTransferHandler we only want to copy a component onto the design window, not link or move
     * @param sourceComponent the sourceComponent
     * @return COPY if the transfer property can be found, otherwise returns
     */
    @Override
    public int getSourceActions(JComponent sourceComponent) {
        return TransferHandler.COPY;
    }

    @Override
    public Transferable createTransferable(JComponent sourceComponent) {
        if (sourceComponent instanceof JList<?> paletteList &&
                paletteList.getSelectedValue() instanceof PaletteItem item &&
                !item.isCategory()) {
            UiContext uiContext = project.getService(UiContext.class);
            if (uiContext.getIkasanModule() == null) {
                LOG.warn("STUDIO: Module should never be null");
            }

            BasicElement ikasanComponent;
            String metapackVersion = uiContext.getIkasanModule().getMetaVersion();
            try {
                if (item.getIkasanPaletteElementViewHandler().getComponentMeta().isFlow()) {
                    ikasanComponent = Flow.flowBuilder().metapackVersion(metapackVersion).build();
                } else {
                    ikasanComponent = FlowElementFactory.createFlowElement(metapackVersion, item.getIkasanPaletteElementViewHandler().getComponentMeta(), null, null, null);
                }
            } catch (StudioBuildException e) {
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.AProblemOccurredTryingToGetTheMetaPackInformation", e.getMessage()));
                return null;
            }

            IkasanFlowUIComponentTransferable newTransferable = new IkasanFlowUIComponentTransferable(ikasanComponent);
            Image dragImage = iconToImage(sourceComponent, ComponentIconProvider.getSmallIcon(item.getIkasanPaletteElementViewHandler().getComponentMeta()));
            if (dragImage != null) {
                setDragImage(dragImage);
            }

            return newTransferable;
        }

        return null;
    }

    private static Image iconToImage(Component component, Icon icon) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        if (icon instanceof ImageIcon imageIcon) {
            return imageIcon.getImage();
        }
        BufferedImage image = UIUtil.createImage(
                component, icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }
    //These methods are invoked for the drop gesture, or the paste action, when the component is the target of the operation

    /**
     * This method is called repeatedly during a drag gesture and returns true if the area below the cursor can accept the
     * transfer, or false if the transfer will be rejected.
     * For example, a flow can be dropped anywhere on the canvas, so it will be true for the whole canvas but
     * a component can only be dropped inside a flow, so it will be false until the mouse is over a flow.
     * @param targetComponent that the mouse is over that has registered this as its Transfer Handler.
     * @param destinationSupportedflavors flavors that the target can handle
     * @return false all the time currently
     */
    @Override
    public boolean canImport(JComponent targetComponent, DataFlavor[] destinationSupportedflavors) {
        // This is the TransferHandler used while hovvering over the Palette itself, we don't want to (currently) import - always false.
        return false;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        return super.importData(support);

    }

    /**
     * Called once by Swing when the whole drag gesture concludes, however it ended: a successful drop, a drop
     * rejected by the target, or the drag being cancelled (e.g. dropped outside the canvas, or Escape pressed).
     * While dragging, {@code DesignerCanvas} colours flow borders to give drop-target feedback (green/red); this
     * is the one reliable place to put that highlighting back to normal once the drag is over.
     */
    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        super.exportDone(source, data, action);
        DesignerCanvas designerCanvas = project.getService(UiContext.class).getDesignerCanvas();
        if (designerCanvas != null) {
            designerCanvas.resetContextSensitiveHighlighting();
        }
    }

//    /**
//     * This method is called on a successful drop (or paste) and initiates the transfer of data to the target component.
//     * This method returns true if the import was successful and false otherwise.
//     * @param targetComponent under the mouse that has registered as being able to receive this flavor of component.
//     * @param t the data object being dragged.
//     * @return true if the import was a success
//     */
//    @Override
//    public boolean importData(JComponent targetComponent, Transferable t) {
//
//        if (targetComponent instanceof JPanel) {
//            if (t.isDataFlavorSupported(ikasanFlowUIComponentFlavor)) {
//                try {
//                    IkasanFlowUIComponentTransferable ikasanFlowUIComponent = (IkasanFlowUIComponentTransferable) t.getTransferData(ikasanFlowUIComponentFlavor);
//                    return true;
//                } catch (UnsupportedFlavorException | IOException ignored) {
//                }
//            }
//        }
//        return false;
//    }

}
