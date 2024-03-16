package org.ikasan.studio.core.model.ikasan.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Capture the meta information for an action resolution. The meta will never change per class, so this is static.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class ExceptionResolutionMeta extends ComponentMeta {
    List<String> exceptionsCaught;
    List<ExceptionAction> actionList;
    private static final Map<String, String> STANDARD_EXCEPTIONS = IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionResolutionMeta.class);

    /**
     * Though this is currently a static class, we expose its instance creation to make it more accessible to the template library
     */
    public ExceptionResolutionMeta() {}

    public static List<String> getStandardExceptionsList() {
        return new ArrayList<>(STANDARD_EXCEPTIONS.keySet());
    }

    public static boolean isValidAction(String action) {
        return true;
//        return ON_EXCEPTION.hasProperty(action);
    }

    public static List<ComponentPropertyMeta> getPropertyMetaListForAction(String action) {
        LOG.error("Not yet implemented");
        return null;
//        return ON_EXCEPTION.getMetadataList(action);
    }
}
