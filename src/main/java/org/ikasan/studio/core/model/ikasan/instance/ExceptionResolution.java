package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.ExceptionResolverMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;

import java.util.List;
import java.util.Map;

@JsonSerialize
@Getter
@Setter
@ToString
public class ExceptionResolution extends BasicElement {
    String exceptionsCaught;
    String theAction;

    public ExceptionResolution(String metapackVersion) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
    }

    @Builder(builderMethodName = "exceptionResolutionBuilder")
    public ExceptionResolution(String metapackVersion, String exceptionsCaught, String theAction, Map<String, ComponentProperty> componentProperties) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
        this.exceptionsCaught = exceptionsCaught;
        this.theAction = theAction;
        this.componentProperties = componentProperties;
    }


    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties metadata for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<ComponentPropertyMeta> getMetaForActionParam(String action) {
        return ExceptionResolverMeta.getPropertyMetaListForAction(action);
    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties metadata for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<ComponentPropertyMeta> getMandatoryProperties(String action) {
        return ExceptionResolverMeta.getPropertyMetaListForAction(action);
    }

    /**
     * For ExceptionResolution, the meta belongs to the ExceptionResolver but the values belong to the resolution
     * In this case alone, set the properties directly
     * @param key of the property
     * @param value of the property
     */
    public void setPropertyValue(ComponentPropertyMeta componentPropertyMeta, String key, Object value) {
        componentProperties.put(key, new ComponentProperty(componentPropertyMeta, value));
    }
}
