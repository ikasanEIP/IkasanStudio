package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.PaletteItem;

import java.util.Arrays;

public class ViewHandlerCache implements ViewHandlerFactory {
    private static final Logger LOG = Logger.getInstance("#ViewHandlerCache");
    private final Project project;

    public ViewHandlerCache(Project project) {
        this.project = project;
    }

    public AbstractViewHandler getInstance(Object component) {
        AbstractViewHandlerIntellij returnAbstractViewHandlerIntellij = null ;
        if (component != null) {
            if (component instanceof PaletteItem) {
                // PaletteItems get a view handler when instantiated.
                returnAbstractViewHandlerIntellij = ((PaletteItem) component).getIkasanPaletteElementViewHandler();
            } else if (component instanceof Module) {
                returnAbstractViewHandlerIntellij = new IkasanModuleViewHandler(project, (Module) component);
            } else if (component instanceof Flow) {
                returnAbstractViewHandlerIntellij = new IkasanFlowViewHandler(project, (Flow) component);
            } else if (component instanceof ExceptionResolver) {
                returnAbstractViewHandlerIntellij = new IkasanFlowExceptionResolverViewHandler((ExceptionResolver) component);
            } else if (component instanceof FlowElement) {
                returnAbstractViewHandlerIntellij = new IkasanFlowComponentViewHandler((FlowElement) component);
            }
        }
        if (returnAbstractViewHandlerIntellij == null) {
            LOG.error("STUDIO: View handler returned null for component " + component);
        }
        return returnAbstractViewHandlerIntellij;
    }

    public static IkasanFlowViewHandler getFlowViewHandler(Project project, Flow flow) {
        ViewHandlerCache viewHandlerCache = project.getService(UiContext.class).getViewHandlerFactory();
        IkasanFlowViewHandler viewHandler = null;
        try {
            viewHandler = ((IkasanFlowViewHandler)flow.getOrCreateViewHandler(viewHandlerCache));
        } catch (StudioException se) {
            LOG.warn("STUDIO: A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }

    public static IkasanFlowComponentViewHandler getFlowComponentViewHandler(Project project, IkasanComponent ikasanComponent) {
        ViewHandlerCache viewHandlerCache = project.getService(UiContext.class).getViewHandlerFactory();
        IkasanFlowComponentViewHandler viewHandler = null;
        BasicElement ikasanBasicElement = (BasicElement)ikasanComponent;
        try {
            viewHandler = ((IkasanFlowComponentViewHandler)ikasanBasicElement.getOrCreateViewHandler(viewHandlerCache));
        } catch (StudioException se) {
            LOG.warn("STUDIO: A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }

    public static AbstractViewHandlerIntellij getAbstractViewHandler(Project project, BasicElement ikasanBasicElement) {
        ViewHandlerCache viewHandlerCache = project.getService(UiContext.class).getViewHandlerFactory();
        AbstractViewHandlerIntellij viewHandler = null;
        try {
            viewHandler = ((AbstractViewHandlerIntellij)ikasanBasicElement.getOrCreateViewHandler(viewHandlerCache));
        } catch (StudioException se) {
            LOG.warn("STUDIO: A studio exception was raised while trying to get the view handlers, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
        }
        return viewHandler;
    }
}
