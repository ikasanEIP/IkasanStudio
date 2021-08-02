package org.ikasan.studio.model.ikasan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ikasan.studio.model.ikasan.IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS;

/**
 * Capture the meta information for an action resolution. The meta will never change per class, so this is static.
 */
public class IkasanExceptionResolutionMeta {
    private static final Map<String, String> STANDARD_EXCEPTIONS = EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
    private static final IkasanComponentType ON_EXCEPTION = IkasanComponentType.ON_EXCEPTION;

    /**
     * Though this is currently a static class, we expose its instance creation to make it more accessible to the template library
     */
    public IkasanExceptionResolutionMeta() {

    }

    public static List<String> getStandardExceptionsList() {
        return new ArrayList<>(STANDARD_EXCEPTIONS.keySet());
    }

    public static boolean isValidAction(String action) {
        return ON_EXCEPTION.hasProperty(action);
    }

    public static List<IkasanComponentPropertyMeta> getPropertyMetaListForAction(String action) {
        return ON_EXCEPTION.getMetadataList(action);
    }

    public static List<String> getActionList() {
        return ON_EXCEPTION.getPropertyNames();
    }

    public static String parseAction(String onExceptionMethod) {
        if (onExceptionMethod != null) {
            return ON_EXCEPTION.getPropertyNames().stream().filter(action -> onExceptionMethod.contains(action)).findFirst().orElse("");
        } else {
            return "";
        }
    }

}
