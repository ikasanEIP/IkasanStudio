package org.ikasan.studio.core.model.ikasan.instance;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
public class ExceptionResolutions extends IkasanObject {
    List<ExceptionResolution> exceptionResolutions;

//    public ExceptionResolutions(String metapackVersion) throws StudioBuildException {
//        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion));
//        exceptionResolutions = Collections.EMPTY_LIST;
//    }
//
//    @Builder(builderMethodName = "exceptionResolutionsBuilder")
//    public ExceptionResolutions(String metapackVersion, List<ExceptionResolution> exceptionResolutions) throws StudioBuildException {
//        super(IkasanComponentLibrary.getExceptionResolverMetaMandatory(metapackVersion));
//        this.exceptionResolutions = exceptionResolutions;
//    }
//
//    /**
//     * Expose the property meta for a given action.
//     * @param action to search for
//     * @return a list if the properties metadata for this action, or an empty list if none exist.
//     */
//    @JsonIgnore
//    public static List<ComponentPropertyMeta> getMetaForActionParams(String action) {
//        return ExceptionResolverMeta.getPropertyMetaListForAction(action);
//    }

}
