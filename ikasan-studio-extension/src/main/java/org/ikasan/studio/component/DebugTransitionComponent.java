package org.ikasan.studio.component;

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;
import org.ikasan.studio.component.utils.DeepCopyUtil;

public abstract class DebugTransitionComponent implements Filter<Object> {
    @Override
    public final Object filter(Object message) throws FilterException {
        debug(DeepCopyUtil.deepCopy(message));
        return message;
    }

    public abstract void debug(Object message) ;
}