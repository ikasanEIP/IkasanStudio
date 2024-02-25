package org.ikasan.studio.model.ikasan.instance;

import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate the ExceptionResolver.
 */

public class ExceptionResolver extends FlowElement {
    //@todo need to split IkasanComponent to nest ExceptionResolver above a protected Map<IkasanComponentPropertyMetaKey, ComponentProperty> configuredProperties; level
    private Map<String, ExceptionResolution> ikasanExceptionResolutionMap = new HashMap<>();  // list of all the exceptions we catch / process

    /**
     * Create an ExceptionResolver
     * @param parent flow that contains this exceptions resolver
     */
    protected ExceptionResolver(Flow parent) {
        super(IkasanComponentLibrary.getExceptionResolver(IkasanComponentLibrary.STD_IKASAN_PACK), parent);
    }

    public Map<String, ExceptionResolution> getIkasanExceptionResolutionMap() {
        return ikasanExceptionResolutionMap;
    }

    public void addExceptionResolution(ExceptionResolution exceptionResolution) {
        ikasanExceptionResolutionMap.put(exceptionResolution.getExceptionsCaught(), exceptionResolution);
    }

    public void resetIkasanExceptionResolutionList() {
        ikasanExceptionResolutionMap = new HashMap<>();
    }

    /**
     * Determine if the Resolver is valid i.e. has more than one resolution.
     * @return true if the Resolver is valid.
     */
    public boolean isValid() {
        return (ikasanExceptionResolutionMap != null && !ikasanExceptionResolutionMap.isEmpty());
    }
}
