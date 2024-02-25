package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.*;
import org.ikasan.studio.ui.model.PaletteItem;

public class ViewHandlerFactory {
    private static final Logger LOG = Logger.getInstance("#ViewHandlerFactory");
    // Static class
    private ViewHandlerFactory() {}

    public static ViewHandler getInstance(Object component) {
        ViewHandler returnViewHandler = null ;
        if (component != null) {
            if (component instanceof PaletteItem) {
//            if (component instanceof FlowElement) {
                returnViewHandler =  new IkasanPaletteElementViewHandler(((PaletteItem) component).getComponentMeta());
            } else if (component instanceof Module) {
                returnViewHandler =  new IkasanModuleViewHandler((Module) component);
            } else if (component instanceof Flow) {
                returnViewHandler =  new IkasanFlowViewHandler((Flow) component);
            } else if (component instanceof FlowElement) {
                returnViewHandler =  new IkasanFlowComponentViewHandler((org.ikasan.studio.model.ikasan.instance.FlowElement) component);
            } else if (component instanceof ExceptionResolver) {
                returnViewHandler =  new IkasanFlowExceptionResolverViewHandler((ExceptionResolver) component);
            }
        }
        if (returnViewHandler == null) {
            LOG.error("View handler returned null for component " + component);
        }
        return returnViewHandler;
    }
}
