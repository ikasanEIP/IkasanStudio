package org.ikasan;

/**
* Splitters take in a single incoming payload and split it into an ordered list of separate outgoing payloads,
* each of which is sent downstream one at a time. Unlike a Router, a Splitter always has exactly one outgoing
* transition - it cannot itself choose between different routes, and it can never be the last component in a
* flow (see org.ikasan.flow.visitorPattern.invoker.SplitterFlowElementInvoker).
*
* IMPORTANT: do not set the input type below to java.lang.Object. Like Broker, Ikasan decides whether this
* Splitter wants just the payload or the full event by first trying split(FlowEvent) and catching a
* ClassCastException if that fails - java.lang.Object accepts anything without ever throwing, so it silently
* locks in "pass the whole FlowEvent" mode instead of the payload you're expecting. Leave the input type as the
* real payload type (e.g. String) to receive just the payload; set it to org.ikasan.spec.flow.FlowEvent instead
* if you need the full event (identifier, timestamp, etc. as well as the payload) - call payload.getPayload()
* inside split() to get the payload out.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be over-written unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.splitting.Splitter;
import org.ikasan.spec.component.splitting.SplitterException;

@org.springframework.stereotype.Component("org.ikasan.MySplitter")
public class MySplitter implements Splitter<java.lang.String, java.lang.String>
{
/**
* @param payload the single incoming payload to split
* @return an ordered list of the outgoing payloads to send downstream, one at a time - must contain at least
* one element, a null or empty list is not valid and throws a SplitterException at runtime
*/
public java.util.List<java.lang.String> split(java.lang.String payload) throws SplitterException
{
//@TODO implement your splitting logic, returning at least one java.lang.String payload for the downstream component
return java.util.List.of();
}
}