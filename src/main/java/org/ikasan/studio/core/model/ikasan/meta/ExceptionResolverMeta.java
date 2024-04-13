package org.ikasan.studio.core.model.ikasan.meta;

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

    /**
     * Though this is currently a static class, we expose its instance creation to make it more accessible to the template library
     */
    public ExceptionResolverMeta() {}

    public static boolean isValidAction(String action) {
        return true;
    }

    public static List<ComponentPropertyMeta> getPropertyMetaListForAction(String action) {
        LOG.error("STUDIO: Not yet implemented");
        return null;
    }

    public ExceptionActionMeta getExceptionActionWithName(String actionName) {
        for (ExceptionActionMeta exceptionActionMeta : actionList) {
            if (exceptionActionMeta.getActionName().equals(actionName)) {
                return exceptionActionMeta;
            }
        }
        return null;
    }
}
