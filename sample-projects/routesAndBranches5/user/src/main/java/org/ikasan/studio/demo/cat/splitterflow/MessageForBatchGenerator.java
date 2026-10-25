package org.ikasan.studio.demo.cat.splitterflow;

/**
 * Brokers enrich the contents of the existing message with additional data or structure in a number of different ways.
 * Request Response Brokers can make calls to other systems such as a database or HTTP(s) RESTful services.
 * Aggregating Brokers consume all incoming messages until a condition is met ie aggregate every 10 messages.
 * Re-Sequencing Brokers consume all incoming messages until a condition is met and then release them messages as a
 * list of newly ordered payloads. This can provide a powerful function when combined with a Splitter as the next component.
 * <p>
 * IMPORTANT: do not set the input type below to java.lang.Object. Ikasan decides whether this Broker wants just
 * the payload or the full event by first trying invoke(FlowEvent) and catching a ClassCastException if that
 * fails - java.lang.Object accepts anything without ever throwing, so it silently locks in "pass the whole
 * FlowEvent" mode instead of the payload you're expecting. Leave the input type as the real payload type (e.g.
 * String) to receive just the payload; set it to org.ikasan.spec.flow.FlowEvent instead if you need the full
 * event (identifier, timestamp, etc. as well as the payload) - call event.getPayload() inside invoke() to get
 * the payload out.
 * <p>
 * This is an auto generated stub. The user is expected to fill in the details of the conversion below.
 * This stub will not be over-written unless the overwrite checkbox is explicitly selected.
 */

import org.ikasan.spec.component.endpoint.Broker;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("org.ikasan.studio.demo.cat.splitterflow.MessageForBatchGenerator")
public class MessageForBatchGenerator implements Broker<JobExecutionContext, List<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(MessageForBatchGenerator.class);
    private static int counter = 0;

    @Override
    public java.util.List<java.lang.String>  invoke(JobExecutionContext payload) throws EndpointException {
        java.util.List<java.lang.String> boblist = new ArrayList<>();
        LOG.debug("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
        if (payload == null) {
            LOG.error("MessageGenerator found a null payload");
        } else {
            String message1 = "Item " + payload.getFireTime() + " A=" + counter;
            String message2 = "Item " + payload.getFireTime() + " B=" + counter;

            boblist.add(message1 + "|" + message1);
            boblist.add(message2 + "|" + message2);
            counter++;
        }
        return boblist;
    }
}