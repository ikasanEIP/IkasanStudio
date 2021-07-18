package org.ikasan.studio.model.ikasan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulate the IkasanExceptionResolver.
 */
public class IkasanExceptionResolver extends IkasanFlowComponent {
    //@todo need to split IkasanComponent to nest IkasanExceptionResolver above a protected Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> configuredProperties; level
    private Map<String, IkasanExceptionResolution> ikasanExceptionResolutionMap = new HashMap<>();

    /**
     * Create an IkasanExceptionResolver
     * @param ikasanExceptionResolutionMap, the exception name is the key
     */
//    public IkasanExceptionResolver() {
//        super(IkasanComponentType.EXCEPTION_RESOLVER);
//        this.ikasanExceptionResolutionMap = ikasanExceptionResolutionMap;
//    }

    /**
     * Create an IkasanExceptionResolver
     * @param parent flow that contains this exceptions resolver
     */
    public IkasanExceptionResolver(IkasanFlow parent) {
        super(IkasanComponentType.EXCEPTION_RESOLVER, parent);
//        this.ikasanExceptionResolutionMap = ikasanExceptionResolutionMap;
    }

    public boolean hasExceptionResolution(String key) {
        return ikasanExceptionResolutionMap.containsKey(key);
    }

    public Map<String, IkasanExceptionResolution> getIkasanExceptionResolutionMap() {
        return ikasanExceptionResolutionMap;
    }

    public void setIkasanExceptionResolutionMap(Map<String, IkasanExceptionResolution> ikasanExceptionResolutionMap) {
        this.ikasanExceptionResolutionMap = ikasanExceptionResolutionMap;
    }

    public void addExceptionResolution(IkasanExceptionResolution ikasanExceptionResolution) {
        ikasanExceptionResolutionMap.put(ikasanExceptionResolution.getTheException(), ikasanExceptionResolution);
    }

    public void resetIkasanExceptionResolutionList(List<IkasanExceptionResolution> ikasanExceptionResolutionList) {
        resetIkasanExceptionResolutionList();
        for (IkasanExceptionResolution ikasanExceptionResolution : ikasanExceptionResolutionList) {
            addExceptionResolution(ikasanExceptionResolution);
        }
    }

    public void resetIkasanExceptionResolutionList() {
        ikasanExceptionResolutionMap = new HashMap<>();
    }
}
