package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

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
     * @param parent flow that contains this exceptions resolver
     */
    public IkasanExceptionResolver(IkasanFlow parent) {
        super(IkasanComponentType.EXCEPTION_RESOLVER, parent);
        this.viewHandler = ViewHandlerFactory.getInstance(this);
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

    /**
     * Determine if the Resolver is valid i.e. has more than one resolution.
     * @return true if the Resolver is valid.
     */
    public boolean isValid() {
        return (ikasanExceptionResolutionMap != null && !ikasanExceptionResolutionMap.isEmpty());
    }
}
