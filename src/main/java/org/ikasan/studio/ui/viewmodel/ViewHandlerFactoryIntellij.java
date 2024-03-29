package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.PaletteItem;

import java.util.Arrays;

public class ViewHandlerFactoryIntellij implements ViewHandlerFactory {
    private static final Logger LOG = Logger.getInstance("#ViewHandlerFactoryIntellij");
    private final String projectKey;

    public ViewHandlerFactoryIntellij(String projectKey) {
        this.projectKey = projectKey;
    }

    public AbstractViewHandler getInstance(Object component) {
        AbstractViewHandlerIntellij returnAbstractViewHandlerIntellij = null ;
        if (component != null) {
            if (component instanceof PaletteItem) {
                returnAbstractViewHandlerIntellij =  new IkasanPaletteElementViewHandler(((PaletteItem) component).getComponentMeta());
            } else if (component instanceof Module) {
                returnAbstractViewHandlerIntellij =  new IkasanModuleViewHandler(projectKey, (Module) component);
            } else if (component instanceof Flow) {
                returnAbstractViewHandlerIntellij =  new IkasanFlowViewHandler(projectKey, (Flow) component);
            } else if (component instanceof FlowElement) {
                returnAbstractViewHandlerIntellij =  new IkasanFlowComponentViewHandler((FlowElement) component);
            } else if (component instanceof ExceptionResolver) {
                returnAbstractViewHandlerIntellij =  new IkasanFlowExceptionResolverViewHandler((ExceptionResolver) component);
            }
        }
        if (returnAbstractViewHandlerIntellij == null) {
            LOG.error("View handler returned null for component " + component);
        }
        return returnAbstractViewHandlerIntellij;
    }

    public static IkasanFlowViewHandler getFlowViewHandler(String projectKey, Flow flow) {
        ViewHandlerFactoryIntellij viewHandlerFactoryIntellij = UiContext.getViewHandlerFactory(projectKey);
        IkasanFlowViewHandler viewHandler = null;
        try {
            viewHandler = ((IkasanFlowViewHandler)flow.getViewHandler(viewHandlerFactoryIntellij));
        } catch (StudioException se) {
            LOG.warn("A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }

    public static IkasanFlowComponentViewHandler getFlowComponentHandler(String projectKey, BasicElement ikasanBasicElement) {
        ViewHandlerFactoryIntellij viewHandlerFactoryIntellij = UiContext.getViewHandlerFactory(projectKey);
        IkasanFlowComponentViewHandler viewHandler = null;
        try {
            viewHandler = ((IkasanFlowComponentViewHandler)ikasanBasicElement.getViewHandler(viewHandlerFactoryIntellij));
        } catch (StudioException se) {
            LOG.warn("A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }

    public static AbstractViewHandlerIntellij getAbstracttHandler(String projectKey, BasicElement ikasanBasicElement) {
        ViewHandlerFactoryIntellij viewHandlerFactoryIntellij = UiContext.getViewHandlerFactory(projectKey);
        AbstractViewHandlerIntellij viewHandler = null;
        try {
            viewHandler = ((AbstractViewHandlerIntellij)ikasanBasicElement.getViewHandler(viewHandlerFactoryIntellij));
        } catch (StudioException se) {
            LOG.warn("A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }
}