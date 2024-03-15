package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.build.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.build.model.ikasan.instance.Flow;
import org.ikasan.studio.build.model.ikasan.instance.FlowElement;
import org.ikasan.studio.build.model.ikasan.instance.Module;
import org.ikasan.studio.ui.model.PaletteItem;

public class ViewHandlerFactory {
    private static final Logger LOG = Logger.getInstance("#ViewHandlerFactory");
    // Static class
    private ViewHandlerFactory() {}

    public static AbstractViewHandler getInstance(Object component) {
        AbstractViewHandler returnAbstractViewHandler = null ;
        if (component != null) {
            if (component instanceof PaletteItem) {
//            if (component instanceof FlowElement) {
                returnAbstractViewHandler =  new IkasanPaletteElementAbstractViewHandler(((PaletteItem) component).getComponentMeta());
            } else if (component instanceof Module) {
                returnAbstractViewHandler =  new IkasanModuleAbstractViewHandler((Module) component);
            } else if (component instanceof Flow) {
                returnAbstractViewHandler =  new IkasanFlowAbstractViewHandler((Flow) component);
            } else if (component instanceof FlowElement) {
                returnAbstractViewHandler =  new IkasanFlowComponentAbstractViewHandler((FlowElement) component);
            } else if (component instanceof ExceptionResolver) {
                returnAbstractViewHandler =  new IkasanFlowExceptionResolverAbstractViewHandler((ExceptionResolver) component);
            }
        }
        if (returnAbstractViewHandler == null) {
            LOG.error("View handler returned null for component " + component);
        }
        return returnAbstractViewHandler;
    }
}
