package org.ikasan.studio.demo.cat.transformtheorder;

import org.ikasan.studio.component.DebugTransitionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The purpose of this component is to allow the developer to set breakpoints in between components and inspect the payload
 */


@Component("org.ikasan.studio.demo.cat.transformtheorder.TransformTheOrderOrderConsumerDebug")

public class TransformTheOrderOrderConsumerDebug extends DebugTransitionComponent {

    /**
     * Allow user to view the message payload.
     * The framework attempts to copy the payload; copying is best-effort, so inspect it without mutation.
     *
     * @param payload the payload or its best-effort copy; may be null
     */
    @Override
    public void debug(Object payload) {
        Logger logger = LoggerFactory.getLogger(getClass());
        String payloadType = payload == null ? "null" : payload.getClass().getName();
// Set your breakpoint on the following line: it executes once per incoming payload.
        logger.debug("Flow debug invoked for payload type {}", payloadType);
    }

}