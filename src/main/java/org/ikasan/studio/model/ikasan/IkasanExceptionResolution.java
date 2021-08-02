package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

public class IkasanExceptionResolution extends IkasanBaseComponent {
    private static final IkasanExceptionResolutionMeta IKASAN_EXCEPTION_RESOLUTION_META = new IkasanExceptionResolutionMeta();
    @JsonIgnore
    IkasanExceptionResolver parent;
    String theException;
    String theAction;
    List<IkasanComponentProperty> params;

    public IkasanExceptionResolution(IkasanExceptionResolver parent) {
        super(IkasanComponentType.ON_EXCEPTION);
        this.parent = parent;
    }

    public IkasanExceptionResolution(String theException, String theAction) {
        this(theException, theAction, Collections.emptyList());
    }

    public IkasanExceptionResolution(String theException, String theAction, List<IkasanComponentProperty> params) {
        super(IkasanComponentType.ON_EXCEPTION);
        this.theException = theException;
        this.theAction = theAction;
        this.params = params;
    }
    public String getTheException() {
        return theException;
    }
    public void setTheException(String theException) {
        this.theException = theException;
    }
    public String getTheAction() {
        return theAction;
    }
    public void setTheAction(String theAction) {
        this.theAction = theAction;
    }
    public static IkasanExceptionResolutionMeta getMeta() {
        return IKASAN_EXCEPTION_RESOLUTION_META;
    }

    /**
     * Getter for the parent of this resolution, note JsonIgnore to prevent circular dependency.
     */
    @JsonIgnore
    public IkasanExceptionResolver getParent() {
        return parent;
    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties meta data for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<IkasanComponentPropertyMeta> getMetaForActionParams(String action) {
        return IkasanExceptionResolutionMeta.getPropertyMetaListForAction(action);
    }
    @JsonIgnore
    public List<IkasanComponentProperty> getParams() {
        if (params == null) {
            return Collections.emptyList();
        } else {
            return params;
        }
    }

    public void setParams(List<IkasanComponentProperty> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "IkasanExceptionResoluation{" +
                "theException=" + theException +
                ", theAction='" + theAction + '\'' +
                ", params=" + params +
                '}';
    }
}
