package org.example.acap4.localfiletogenericflow;

import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.component.endpoint.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Producers push a payload to a protocol endpoint, typically the last component of a flow or of a route
 * within it.
 *
 * @author Ikasan Development Team
 */

@Component("org.example.acap4.localfiletogenericflow.MyGenericProducer")

public class MyGenericProducer implements Producer<List<File>> {
    private static final Logger LOG = LoggerFactory.getLogger(MyGenericProducer.class);

    /**
     * Push the payload to your protocol endpoint.
     *
     * @param payload
     * @throws EndpointException
     */
    @Override
    public void invoke(List<File> payload) throws EndpointException {
// Enable DEBUG logging for this package to see arrivals without logging message contents.
        LOG.info("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
    }
}