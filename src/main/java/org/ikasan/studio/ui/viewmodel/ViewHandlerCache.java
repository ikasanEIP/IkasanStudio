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
            switch (component) {
                case PaletteItem paletteItem ->
                    // PaletteItems get a view handler when instantiated.
                        returnAbstractViewHandlerIntellij = paletteItem.getIkasanPaletteElementViewHandler();
                case Module module -> returnAbstractViewHandlerIntellij = new IkasanModuleViewHandler(project, module);
                case Flow flow -> returnAbstractViewHandlerIntellij = new IkasanFlowViewHandler(project, flow);
                case ExceptionResolver exceptionResolver ->
                        returnAbstractViewHandlerIntellij = new IkasanFlowExceptionResolverViewHandler(exceptionResolver);
                case FlowElement flowElement ->
                        returnAbstractViewHandlerIntellij = new IkasanFlowComponentViewHandler(flowElement);
                default -> {
                }
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
