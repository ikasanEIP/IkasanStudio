package org.ikasan.studio.ui.model;

import lombok.Data;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

@Data
public class PaletteItem {
    private ComponentMeta componentMeta;
    private IkasanPaletteElementViewHandler ikasanPaletteElementViewHandler;
    private boolean category = false;

    /**
     * Create a Category Palette Item i.e. one that merely displays a title and does not move
     * @param categoryName to be displayed
     */
    public PaletteItem(String categoryName) {
        this.componentMeta = ComponentMeta.builder()
            .name(categoryName)
            .componentType("dummy")
            .implementingClass("dummy")
            .build();
        componentMeta.setName(categoryName);
        ikasanPaletteElementViewHandler = new IkasanPaletteElementViewHandler(componentMeta);
        category = true;
    }

    /**
     * Create a Palette item to represent an Ikasan component
     * @param componentMeta to create
     */
    public PaletteItem(ComponentMeta componentMeta) {
        this.componentMeta = componentMeta;
        this.ikasanPaletteElementViewHandler = new IkasanPaletteElementViewHandler(componentMeta);
    }

}
