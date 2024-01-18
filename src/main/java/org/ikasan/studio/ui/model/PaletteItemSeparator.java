package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.IkasanPaletteElementViewHandler;

public class PaletteItemSeparator extends PaletteItem {

    public PaletteItemSeparator(String title) {
        flowElement = FlowElement.getDummyFlowElement();
        flowElement.setPropertyValue("title", title);
        ikasanFlowElementViewHandler = new IkasanPaletteElementViewHandler(flowElement);
    }
}
