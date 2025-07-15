package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.serialization.ExceptionResolverSerializer;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The Exception Resolver owns the view handler andd has a list of exception resolutions
 */

@JsonSerialize(using = ExceptionResolverSerializer.class)
@Getter
@Setter
@ToString
public class ExceptionResolver extends FlowElement {
    private static final Logger LOG = LoggerFactory.getLogger(FlowElement.class);
    private Map<String, ExceptionResolution> ikasanExceptionResolutionMap ;
    public ExceptionResolver(String metapackVersion, Flow containingFlow) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), containingFlow, null, null, null);
        ikasanExceptionResolutionMap = Collections.EMPTY_MAP;
    }

    @Builder(builderMethodName = "exceptionResolverBuilder")
    public ExceptionResolver(String metapackVersion, Flow containingFlow, Map<String, ExceptionResolution> ikasanExceptionResolutionMap) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), containingFlow, null, null, null);
        this.ikasanExceptionResolutionMap = ikasanExceptionResolutionMap;
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

    /**
     * Used by ftl
     * @return the list of exception resolutions
     */
    public List<ExceptionResolution> getExceptionResolutionList() {
        List<ExceptionResolution> exceptionResolutions = new ArrayList<>();
        if (ikasanExceptionResolutionMap!=null && !ikasanExceptionResolutionMap.isEmpty()) {
            exceptionResolutions.addAll(ikasanExceptionResolutionMap.values());
        }
        return exceptionResolutions;
    }

    /**
     * The intent is to clone the existing ExceptionResolver but to a different meta-pack metapackVersion.
     * @param metapackVersion
     * @return the cloned module with the new meta pack version
     * @throws StudioBuildException when cloning is not possible.
     */
    public ExceptionResolver cloneToVersion(String metapackVersion, Flow containingFlow) throws StudioBuildException {
        if (metapackVersion == null || metapackVersion.isBlank()) {
            LOG.error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
            return null;
        }
        ExceptionResolver clonedExceptionResolver = new ExceptionResolver(metapackVersion, containingFlow, this.ikasanExceptionResolutionMap);
        super.cloneToVersion(clonedExceptionResolver);
        return clonedExceptionResolver;
    }

    @Override
    public String toSimpleString() {
        StringBuilder builder = new StringBuilder();
        if (ikasanExceptionResolutionMap != null && !ikasanExceptionResolutionMap.isEmpty()) {
            for(Map.Entry<String, ExceptionResolution> resolution : ikasanExceptionResolutionMap.entrySet()) {
                builder.append(resolution.getKey()).append("->").append(resolution.getValue().toSimpleString()).append(",");
            }
        }
        return "[" +
                super.toSimpleString() +
                ", ikasanExceptionResolutionMap [" + builder + "]" +
                ']';
    }
}
