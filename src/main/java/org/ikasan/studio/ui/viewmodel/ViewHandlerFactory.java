package org.ikasan.studio.ui.viewmodel;

import org.ikasan.studio.model.ikasan.IkasanExceptionResolver;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;

public class ViewHandlerFactory {

    // Static class
    private ViewHandlerFactory() {}

    public static ViewHandler getInstance(Object component) {
        if (component == null) {
            return null;
        } else if (component instanceof IkasanFlowUIComponent) {
            return new IkasanPaletteElementViewHandler((IkasanFlowUIComponent) component);
        } else if (component instanceof IkasanModule) {
            return new IkasanModuleViewHandler((IkasanModule) component);
        } else if (component instanceof IkasanFlow) {
            return new IkasanFlowViewHandler((IkasanFlow) component);
        } else if (component instanceof IkasanFlowComponent) {
            return new IkasanFlowComponentViewHandler((IkasanFlowComponent) component);
        } else if (component instanceof IkasanExceptionResolver) {
            return new IkasanFlowExceptionResolverViewHandler((IkasanExceptionResolver) component);
        } else {
            return null;
        }
    }
}
