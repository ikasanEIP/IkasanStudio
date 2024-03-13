package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.ExceptionResolutionMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper=true)
public class ExceptionResolution extends IkasanObject {
//    private static final ExceptionResolutionMeta IKASAN_EXCEPTION_RESOLUTION_META = new ExceptionResolutionMeta();
    @JsonIgnore
ExceptionResolver parent;
    String exceptionsCaught;
    String theAction;
    List<ComponentProperty> params;

    public ExceptionResolution(ExceptionResolver parent) {
        super(IkasanComponentLibrary.getOnException(IkasanComponentLibrary.STD_IKASAN_PACK));
        this.parent = parent;
    }

    /**
     * Getter for the parent of this resolution, note JsonIgnore to prevent circular dependency.
     */
    @JsonIgnore
    public ExceptionResolver getParent() {
        return parent;
    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties metadata for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<ComponentPropertyMeta> getMetaForActionParams(String action) {
        return ExceptionResolutionMeta.getPropertyMetaListForAction(action);
    }
    @JsonIgnore
    public List<ComponentProperty> getParams() {
        return Objects.requireNonNullElse(params, Collections.emptyList());
    }
}
