package org.ikasan.studio.ui.model;

import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItemSeparator extends PaletteItem {

    public PaletteItemSeparator(String title) {
        ikasanFlowUIComponent = new IkasanFlowUIComponent(title);
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(ikasanFlowUIComponent);
    }
}
