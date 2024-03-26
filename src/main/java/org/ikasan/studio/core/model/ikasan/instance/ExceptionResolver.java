package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.serialization.ExceptionResolverSerializer;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Exception Resolver owns the view handler andd has a list of exception resolutions
 */

@JsonSerialize(using = ExceptionResolverSerializer.class)
@Data
@EqualsAndHashCode(callSuper=true)
public class ExceptionResolver extends FlowElement {
    private Map<String, ExceptionResolution> ikasanExceptionResolutionMap = new HashMap<>();  // list of all the exceptions we catch / process

    public ExceptionResolver(String metapackVersion, Flow containingFlow) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), containingFlow, null);
        ikasanExceptionResolutionMap = Collections.EMPTY_MAP;
    }

    @Builder(builderMethodName = "exceptionResolverBuilder")
    public ExceptionResolver(String metapackVersion, Flow containingFlow, Map<String, ExceptionResolution> ikasanExceptionResolutionMap) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), containingFlow, null);
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
}
