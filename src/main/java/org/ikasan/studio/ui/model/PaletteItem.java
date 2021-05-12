package org.ikasan.studio.ui.model;

import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItem {
    IkasanFlowUIComponent ikasanFlowUIComponent;
    IkasanPaletteElementViewHandler ikasanFlowElementViewHandler;
    public IkasanPaletteElementViewHandler getIkasanFlowElementViewHandler() {
        return ikasanFlowElementViewHandler;
    }
    public IkasanFlowUIComponent getIkasanFlowUIComponent() {
        return ikasanFlowUIComponent;
    }
}
