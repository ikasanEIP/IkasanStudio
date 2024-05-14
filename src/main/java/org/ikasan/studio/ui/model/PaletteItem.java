package org.ikasan.studio.ui.model;

import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

@Getter
@Setter
public class PaletteItem {
    private IkasanPaletteElementViewHandler ikasanPaletteElementViewHandler;
    private boolean category = false;

    /**
     * Create a Category Palette Item i.e. one that merely displays a title and does not move
     * @param categoryName to be displayed
     */
    public PaletteItem(String categoryName, final ComponentMeta componentMeta) {
        ComponentMeta clonedComponentMeta = componentMeta.toBuilder()
                .name(categoryName)
                .helpText("")
                .build();
        ikasanPaletteElementViewHandler = new IkasanPaletteElementViewHandler(clonedComponentMeta);
        category = true;
    }

    /**
     * Create a Palette item to represent an Ikasan component
     * @param componentMeta to create
     */
    public PaletteItem(ComponentMeta componentMeta) {
        this.ikasanPaletteElementViewHandler = new IkasanPaletteElementViewHandler(componentMeta);
    }
}
