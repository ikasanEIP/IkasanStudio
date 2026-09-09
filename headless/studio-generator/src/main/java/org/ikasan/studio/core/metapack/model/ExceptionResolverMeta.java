package org.ikasan.studio.core.metapack.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Capture the meta information for an action resolution. The meta will never change per class, so this is static.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class ExceptionResolverMeta extends ComponentMeta {
    List<String> exceptionsCaught;
    List<ExceptionActionMeta> actionList;
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionResolverMeta.class);

    // Stub pending real validation logic - see the commented-out getPropertyMetaListForAction() below, which
    // notes the same "Not yet implemented" gap. Kept as a genuine parameter (not removed) since it's exactly
    // what a real implementation would need to validate against.
    @SuppressWarnings("unused")
    public static boolean isValidAction(String action) {
        return true;
    }

//    public static List<ComponentPropertyMeta> getPropertyMetaListForAction(String action) {
//        LOG.error("STUDIO: Not yet implemented");
//        return null;
//    }

    public ExceptionActionMeta getExceptionActionWithName(String actionName) {
        for (ExceptionActionMeta exceptionActionMeta : actionList) {
            if (exceptionActionMeta.getActionName().equals(actionName)) {
                return exceptionActionMeta;
            }
        }
        return null;
    }
}
