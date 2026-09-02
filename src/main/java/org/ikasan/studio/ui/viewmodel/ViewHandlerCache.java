package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.IkasanComponent;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.PaletteItem;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Project/designer-scoped owner of the UI representation for each domain object. Identity keys are deliberate:
 * two distinct model objects may currently compare equal, but must never share mutable dimensions or PSI targets.
 */
public final class ViewHandlerCache {
    private static final Logger LOG = Logger.getInstance("#ViewHandlerCache");

    private final Project project;
    private final Map<Object, AbstractViewHandlerIntellij> handlers = new IdentityHashMap<>();

    public ViewHandlerCache(Project project) {
        this.project = project;
    }

    public synchronized AbstractViewHandlerIntellij getOrCreate(Object component) {
        if (component == null) {
            return null;
        }
        AbstractViewHandlerIntellij existing = handlers.get(component);
        if (existing != null) {
            return existing;
        }
        AbstractViewHandlerIntellij created = create(component);
        if (created == null) {
            LOG.error("STUDIO: View handler returned null for component " + component);
            return null;
        }
        handlers.put(component, created);
        return created;
    }

    private AbstractViewHandlerIntellij create(Object component) {
        if (component instanceof PaletteItem paletteItem) {
            return paletteItem.getIkasanPaletteElementViewHandler();
        }
        if (component instanceof Module module) {
            return new IkasanModuleViewHandler(project, module);
        }
        if (component instanceof Flow flow) {
            return new IkasanFlowViewHandler(project, flow);
        }
        if (component instanceof ExceptionResolver exceptionResolver) {
            return new IkasanFlowExceptionResolverViewHandler(exceptionResolver);
        }
        if (component instanceof FlowElement flowElement) {
            return new IkasanFlowComponentViewHandler(flowElement);
        }
        return null;
    }

    /** Disposes every UI handler exactly once and releases all domain-object references. */
    public synchronized void clear() {
        Set<AbstractViewHandlerIntellij> uniqueHandlers =
                Collections.newSetFromMap(new IdentityHashMap<>());
        uniqueHandlers.addAll(handlers.values());
        handlers.clear();
        uniqueHandlers.forEach(AbstractViewHandlerIntellij::dispose);
    }

    public synchronized int size() {
        return handlers.size();
    }

    public static IkasanFlowViewHandler getFlowViewHandler(Project project, Flow flow) {
        return handler(project, flow, IkasanFlowViewHandler.class);
    }

    public static IkasanFlowComponentViewHandler getFlowComponentViewHandler(
            Project project, IkasanComponent component) {
        return handler(project, component, IkasanFlowComponentViewHandler.class);
    }

    public static AbstractViewHandlerIntellij getAbstractViewHandler(
            Project project, BasicElement element) {
        return handler(project, element, AbstractViewHandlerIntellij.class);
    }

    private static <T extends AbstractViewHandlerIntellij> T handler(
            Project project, Object component, Class<T> expectedType) {
        ViewHandlerCache cache = project.getService(UiContext.class).getViewHandlerFactory();
        if (cache == null) {
            LOG.warn("STUDIO: View handler requested without an active designer");
            return null;
        }
        AbstractViewHandlerIntellij handler = cache.getOrCreate(component);
        if (!expectedType.isInstance(handler)) {
            if (handler != null) {
                LOG.error("STUDIO: Unexpected view handler type " + handler.getClass().getName()
                        + " for component " + component);
            }
            return null;
        }
        return expectedType.cast(handler);
    }
}
