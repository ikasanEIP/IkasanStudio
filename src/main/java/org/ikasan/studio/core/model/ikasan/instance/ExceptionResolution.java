package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.ExceptionResolverMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;

import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper=true)
public class ExceptionResolution extends BasicElement {
//    private static final ExceptionResolverMeta IKASAN_EXCEPTION_RESOLUTION_META = new ExceptionResolverMeta();
    @JsonIgnore
//    ExceptionResolver parent;
    String exceptionsCaught;
    String theAction;
//    List<ComponentProperty> params;


//    public ExceptionResolution(String metapackVersion, ExceptionResolver parent) throws StudioBuildException {
    public ExceptionResolution(String metapackVersion) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
//        this.parent = parent;
    }

    @Builder(builderMethodName = "exceptionResolutionBuilder")
    public ExceptionResolution(String metapackVersion, String exceptionsCaught, String theAction, Map<String, ComponentProperty> configuredProperties) throws StudioBuildException {
        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion), null);
        this.exceptionsCaught = exceptionsCaught;
        this.theAction = theAction;
        this.configuredProperties = configuredProperties;
    }


//    /**
//     * Getter for the parent of this resolution, note JsonIgnore to prevent circular dependency.
//     */
//    @JsonIgnore
//    public ExceptionResolver getParent() {
//        return parent;
//    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties metadata for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<ComponentPropertyMeta> getMetaForActionParams(String action) {
//        return ExceptionResolverMeta.get// xxx
        return ExceptionResolverMeta.getPropertyMetaListForAction(action);
    }
//    @JsonIgnore
//    public List<ComponentProperty> getParams() {
//        return Objects.requireNonNullElse(params, Collections.emptyList());
//    }
}
