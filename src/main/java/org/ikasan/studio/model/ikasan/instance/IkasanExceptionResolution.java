package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolutionMeta;

import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
public class IkasanExceptionResolution extends IkasanBaseElement {
//    private static final IkasanExceptionResolutionMeta IKASAN_EXCEPTION_RESOLUTION_META = new IkasanExceptionResolutionMeta();
    @JsonIgnore
    IkasanExceptionResolver parent;
    String exceptionsCaught;
    String theAction;
    List<IkasanComponentPropertyInstance> params;

    public IkasanExceptionResolution(IkasanExceptionResolver parent) {
        super(IkasanComponentLibrary.getOnException(IkasanComponentLibrary.STD_IKASAN_PACK));
        this.parent = parent;
    }

//    public static IkasanExceptionResolutionMeta getMeta() {
//        return IKASAN_EXCEPTION_RESOLUTION_META;
//    }
    public static IkasanExceptionResolutionMeta getMeta() {
        return null;
    }

    /**
     * Getter for the parent of this resolution, note JsonIgnore to prevent circular dependency.
     */
    @JsonIgnore
    public IkasanExceptionResolver getParent() {
        return parent;
    }

    /**
     * Expose the property meta for a given action.
     * @param action to search for
     * @return a list if the properties meta data for this action, or an empty list if none exist.
     */
    @JsonIgnore
    public static List<IkasanComponentPropertyMeta> getMetaForActionParams(String action) {
        return IkasanExceptionResolutionMeta.getPropertyMetaListForAction(action);
    }
    @JsonIgnore
    public List<IkasanComponentPropertyInstance> getParams() {
        if (params == null) {
            return Collections.emptyList();
        } else {
            return params;
        }
    }
}
