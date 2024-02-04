package org.ikasan.studio.model.ikasan.meta;

import com.intellij.openapi.diagnostic.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ikasan.studio.model.ikasan.meta.IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS;

/**
 * Capture the meta information for an action resolution. The meta will never change per class, so this is static.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class IkasanExceptionResolutionMeta extends IkasanComponentMeta {
    List<String> exceptionsCaught;
    List<ExceptionAction> actionList;
    private static final Map<String, String> STANDARD_EXCEPTIONS = EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
    private static final Logger LOG = Logger.getInstance("#IkasanExceptionResolutionMeta");

    /**
     * Though this is currently a static class, we expose its instance creation to make it more accessible to the template library
     */
    public IkasanExceptionResolutionMeta() {}

    public static List<String> getStandardExceptionsList() {
        return new ArrayList<>(STANDARD_EXCEPTIONS.keySet());
    }

    public static boolean isValidAction(String action) {
        return true;
//        return ON_EXCEPTION.hasProperty(action);
    }

    public static List<IkasanComponentPropertyMeta> getPropertyMetaListForAction(String action) {
        LOG.error("Not yet implemented");
        return null;
//        return ON_EXCEPTION.getMetadataList(action);
    }
}
