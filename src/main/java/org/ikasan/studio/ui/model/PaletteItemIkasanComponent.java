package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItemIkasanComponent extends PaletteItem {

    public PaletteItemIkasanComponent(FlowElement flowElement) {
        this.flowElement = flowElement;
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(flowElement);
    }
}
