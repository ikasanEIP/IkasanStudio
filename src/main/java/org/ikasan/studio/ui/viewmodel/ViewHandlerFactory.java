package org.ikasan.studio.ui.viewmodel;

import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.*;

public class ViewHandlerFactory {

    // Static class
    private ViewHandlerFactory() {}

    public static ViewHandler getInstance(IkasanElement component) {
        if (component == null) {
            return null;
        } else if (component instanceof FlowElement) {
            return new IkasanPaletteElementViewHandler(component.getIkasanComponentMeta());
        } else if (component instanceof Module) {
            return new IkasanModuleViewHandler((Module) component);
        } else if (component instanceof Flow) {
            return new IkasanFlowViewHandler((Flow) component);
        } else if (component instanceof org.ikasan.studio.model.ikasan.instance.FlowElement) {
            return new IkasanFlowComponentViewHandler((org.ikasan.studio.model.ikasan.instance.FlowElement) component);
        } else if (component instanceof IkasanExceptionResolver) {
            return new IkasanFlowExceptionResolverViewHandler((IkasanExceptionResolver) component);
        } else {
            return null;
        }
    }
}
