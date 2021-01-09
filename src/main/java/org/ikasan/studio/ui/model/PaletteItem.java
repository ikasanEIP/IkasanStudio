package org.ikasan.studio.ui.model;

import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItem {
    IkasanFlowUIComponent ikasanFlowUIComponent;
    IkasanPaletteElementViewHandler ikasanFlowElementViewHandler;

    public PaletteItem(IkasanFlowUIComponent ikasanFlowUIComponent) {
        this.ikasanFlowUIComponent = ikasanFlowUIComponent;
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(ikasanFlowUIComponent);
    }

    public IkasanPaletteElementViewHandler getIkasanFlowElementViewHandler() {
        return ikasanFlowElementViewHandler;
    }

    public IkasanFlowUIComponent getIkasanFlowUIComponent() {
        return ikasanFlowUIComponent;
    }
}
