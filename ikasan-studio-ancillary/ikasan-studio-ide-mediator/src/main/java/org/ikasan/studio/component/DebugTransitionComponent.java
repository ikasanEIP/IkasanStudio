package org.ikasan.studio.component;

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;
import org.ikasan.studio.component.utils.DeepCopyUtil;

/**
 * This class is used to support debug components in flows.
 */
public abstract class DebugTransitionComponent implements Filter<Object> {
    @Override
    public final Object filter(Object message) throws FilterException {
        debug(DeepCopyUtil.deepCopy(message));
        return message;
    }

    /**
     * Expose the payload for debugging purposes
     * @param message the message payload
     */
    public abstract void debug(Object message) ;
}