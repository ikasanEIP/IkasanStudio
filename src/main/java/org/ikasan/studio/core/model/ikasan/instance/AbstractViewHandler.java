package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.StudioException;

/**
 * This interface allows domain objects to be handled easily when used within a UI (Intellij, Eclipse, Swing etc)
 */
public abstract class AbstractViewHandler {
    // The view handler could be thought of as polluting the buildRouteTree namespace since it serves no purpose
    // or value here. However, having a reference handle here means we can plug in the view handlers
    // very easily when the object is used in the UI space, otherwise we would need to deal with very complex
    // parallel structures. Including the handle here is the lesser evil.
    @JsonIgnore
    protected AbstractViewHandler viewHandler;

    /**
     * The scope of the getter forces access via the view handler factory
     * This will allow each UI domain opportunity to provide what ever type of view handler it requires
     * @return a view handler appropriate for the UI that the domain object is being controlled in.
     */
    private AbstractViewHandler getOrCreateViewHandler() {
        return viewHandler;
    }
    /**
     * The scope of the getter forces access via the view handler factory
     * This will allow each UI domain opportunity to provide what ever type of view handler it requires
     */
    private void setViewHandler(AbstractViewHandler viewHandler) {
        this.viewHandler = viewHandler;
    }

    public AbstractViewHandler getOrCreateViewHandler(ViewHandlerFactory viewHandlerFactory) throws StudioException {
        if (viewHandlerFactory == null) {
            throw new StudioException("View Handler Factory must be provided");
        }
        if (viewHandler == null) {
            viewHandler = viewHandlerFactory.getInstance(this);
        }
        if (viewHandler == null) {
            throw new StudioException("View Handler request must never fail");
        }

        return viewHandler;
    }
}
