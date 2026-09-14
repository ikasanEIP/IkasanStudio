package org.ikasan.studio.core.model.ikasan.instance;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;

import java.util.Map;

@Getter
@Setter
@ToString
public class ExceptionResolution extends BasicElement {
    String exceptionsCaught;
    String theAction;

    public ExceptionResolution(String metapackVersion) throws StudioBuildException {
        super(ComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
    }

    @Builder(builderMethodName = "exceptionResolutionBuilder")
    public ExceptionResolution(String metapackVersion, String exceptionsCaught, String theAction, Map<String, ComponentProperty> componentProperties) throws StudioBuildException {
        super(ComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
        this.exceptionsCaught = exceptionsCaught;
        this.theAction = theAction;
        this.componentProperties = componentProperties;
    }

    /**
     * For ExceptionResolution, the meta belongs to the ExceptionResolver, but the values belong to the resolution
     * In this case alone, set the properties directly
     * @param key of the property
     * @param value of the property
     */
    public void setPropertyValue(ComponentPropertyMeta componentPropertyMeta, String key, Object value) {
        componentProperties.put(key, new ComponentProperty(componentPropertyMeta, value));
    }
}
