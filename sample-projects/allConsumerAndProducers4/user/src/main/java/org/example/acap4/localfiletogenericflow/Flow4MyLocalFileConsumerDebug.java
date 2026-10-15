package org.example.acap4.localfiletogenericflow;

/**
 * The purpose of this component is to allow the developer to set breakpoints in between components and inspect the payload
 */

import org.springframework.stereotype.Component;
import org.ikasan.studio.component.DebugTransitionComponent;

@Component("org.example.acap4.localfiletogenericflow.Flow4MyLocalFileConsumerDebug")

public class Flow4MyLocalFileConsumerDebug extends DebugTransitionComponent {

    /**
     * Allow user to view the message payload.
     * The framework will always attempt to supply copy of the payload rather than the payload itself.
     * @param payload is a copy of the payload.
     */
    public void debug(Object payload) {
// Add breakpoints here
        System.out.println("Flow Debug Invoked " + payload.toString());
    }

}