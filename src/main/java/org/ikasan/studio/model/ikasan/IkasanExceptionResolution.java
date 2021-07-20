package org.ikasan.studio.model.ikasan;

import java.util.Collections;
import java.util.List;

public class IkasanExceptionResolution extends IkasanBaseComponent {
    private static IkasanExceptionResolutionMeta IKASAN_EXCEPTION_RESOLUTION_META = new IkasanExceptionResolutionMeta();
    IkasanExceptionResolver parent;
    String theException;
    String theAction;
    List<IkasanComponentProperty> params;

    public IkasanExceptionResolution(IkasanExceptionResolver parent) {
        super(IkasanComponentType.ON_EXCEPTION);
        this.parent = parent;
    }

    public IkasanExceptionResolution(String theException, String theAction) {
        this(theException, theAction, Collections.EMPTY_LIST);
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

    public IkasanExceptionResolver getParent() {
        return parent;
    }

//    /**
//     * Expose the property meta for the action component of this class.
//     * @return a list if the properties meta data for this action, or an empty list if none exist.
//     */
//    public List<IkasanComponentPropertyMeta> getMetaForActionParams() {
//        return IKASAN_EXCEPTION_RESOLUTION_META.getPropertyMetaListForAction(theAction);
//    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties meta data for this action, or an empty list if none exist.
     */
    public static List<IkasanComponentPropertyMeta> getMetaForActionParams(String action) {
        return IKASAN_EXCEPTION_RESOLUTION_META.getPropertyMetaListForAction(action);
    }

    public List<IkasanComponentProperty> getParams() {
        return params;
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
