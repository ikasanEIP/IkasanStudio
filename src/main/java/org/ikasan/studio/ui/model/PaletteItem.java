package org.ikasan.studio.ui.model;

import lombok.Data;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

@Data
public class PaletteItem {
//    FlowElement flowElement;
    private IkasanComponentMeta ikasanComponentMeta;
    private IkasanPaletteElementViewHandler ikasanFlowElementViewHandler;
    private boolean category = false;

    /**
     * Create a Category Palette Item i.e. one that merely displays a title and does not move
     * @param categoryName to be displayed
     */
    public PaletteItem(String categoryName) {
        this.ikasanComponentMeta = new IkasanComponentMeta();
        ikasanComponentMeta.setName(categoryName);
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(ikasanComponentMeta);
        category = true;
    }

    /**
     * Create a Palette item to represent an Ikasan component
     * @param ikasanComponentMeta to create
     */
    public PaletteItem(IkasanComponentMeta ikasanComponentMeta) {
        this.ikasanComponentMeta = ikasanComponentMeta;
        this.ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(ikasanComponentMeta);
    }

}
