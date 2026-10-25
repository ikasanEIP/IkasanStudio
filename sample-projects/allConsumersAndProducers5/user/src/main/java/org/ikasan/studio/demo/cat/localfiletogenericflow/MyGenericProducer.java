package org.ikasan.studio.demo.cat.localfiletogenericflow;

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
 *
 */

@Component("org.ikasan.studio.demo.cat.localfiletogenericflow.MyGenericProducer")

public class MyGenericProducer implements Producer<List<File>> {
    private static final Logger LOG = LoggerFactory.getLogger(MyGenericProducer.class);

    /**
     * Push the payload to your protocol endpoint.
     *
     * @param payload - the message payload to send to the endpoint
     * @throws EndpointException if the payload cannot be delivered,
     *                           for example because the endpoint is unavailable or rejects the request
     */
    @Override
    public void invoke(List<File> payload) throws EndpointException {
// Enable DEBUG logging for this package to see arrivals without logging message contents.
        LOG.debug("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
    }
}