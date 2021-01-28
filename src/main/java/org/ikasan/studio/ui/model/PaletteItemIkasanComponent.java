package org.ikasan.studio.ui.model;

import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItemIkasanComponent extends PaletteItem {

    public PaletteItemIkasanComponent(IkasanFlowUIComponent ikasanFlowUIComponent) {
        this.ikasanFlowUIComponent = ikasanFlowUIComponent;
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(ikasanFlowUIComponent);
    }
}
