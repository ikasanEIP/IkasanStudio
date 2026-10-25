package org.ikasan.studio.demo.cat.splitterflow;

/**
 * Splitters take in a single incoming payload and split it into an ordered list of separate outgoing payloads,
 * each of which is sent downstream one at a time. Unlike a Router, a Splitter always has exactly one outgoing
 * transition - it cannot itself choose between different routes, and it can never be the last component in a
 * flow (see org.ikasan.flow.visitorPattern.invoker.SplitterFlowElementInvoker).
 * <p>
 * IMPORTANT: do not set the input type below to java.lang.Object. Like Broker, Ikasan decides whether this
 * Splitter wants just the payload or the full event by first trying split(FlowEvent) and catching a
 * ClassCastException if that fails - java.lang.Object accepts anything without ever throwing, so it silently
 * locks in "pass the whole FlowEvent" mode instead of the payload you're expecting. Leave the input type as the
 * real payload type (e.g. String) to receive just the payload; set it to org.ikasan.spec.flow.FlowEvent instead
 * if you need the full event (identifier, timestamp, etc. as well as the payload) - call event.getPayload()
 * inside split() to get the payload out.
 * <p>
 * This is an auto generated stub. The user is expected to fill in the details of the conversion below.
 * This stub will not be over-written unless the overwrite checkbox is explicitly selected.
 */

import org.ikasan.spec.component.splitting.Splitter;
import org.ikasan.spec.component.splitting.SplitterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("org.ikasan.studio.demo.cat.splitterflow.StandardSplitterListToString")
public class StandardSplitterListToString implements Splitter<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(StandardSplitterListToString.class);
    /**
     * Split the incoming payload into outgoing payloads for the next component.
     *
     * @param payload the incoming payload to split
     * @return an ordered list of the outgoing payloads to send downstream, one at a time - must contain at least
     * one element, a null or empty list is not valid and throws a SplitterException at runtime
     * @throws SplitterException if the payload cannot be split into valid outgoing messages
     */
    public List<String> split(String payload) throws SplitterException {
        List<String> messageList = new ArrayList<>();
        if (payload == null) {
            LOG.error("SplitPipeMessageInto2 payload was null");
        } else {
            String[] messages = payload.split("\\|");
            messageList.addAll(Arrays.asList(messages));
        }
        return messageList;
    }
}