package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItem {
    FlowElement flowElement;
    IkasanPaletteElementViewHandler ikasanFlowElementViewHandler;
    public IkasanPaletteElementViewHandler getIkasanFlowElementViewHandler() {
        return ikasanFlowElementViewHandler;
    }
    public FlowElement getFlowElement() {
        return flowElement;
    }
}
