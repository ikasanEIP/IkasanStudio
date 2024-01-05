package org.ikasan.studio.ui.viewmodel;

import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolver;
import org.ikasan.studio.model.ikasan.Flow;
import org.ikasan.studio.model.ikasan.FlowElement;
import org.ikasan.studio.model.ikasan.Module;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;

public class ViewHandlerFactory {

    // Static class
    private ViewHandlerFactory() {}

    public static ViewHandler getInstance(Object component) {
        if (component == null) {
            return null;
        } else if (component instanceof IkasanFlowUIComponent) {
            return new IkasanPaletteElementViewHandler((IkasanFlowUIComponent) component);
        } else if (component instanceof Module) {
            return new IkasanModuleViewHandler((Module) component);
        } else if (component instanceof Flow) {
            return new IkasanFlowViewHandler((Flow) component);
        } else if (component instanceof FlowElement) {
            return new IkasanFlowComponentViewHandler((FlowElement) component);
        } else if (component instanceof IkasanExceptionResolver) {
            return new IkasanFlowExceptionResolverViewHandler((IkasanExceptionResolver) component);
        } else {
            return null;
        }
    }
}
