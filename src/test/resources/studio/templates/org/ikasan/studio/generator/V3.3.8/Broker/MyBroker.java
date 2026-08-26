package org.ikasan;

/**
* Brokers enrich the contents of the existing message with additional data or structure in a number of different ways.
* Request Response Brokers can make calls to other systems such as a database or HTTP(s) RESTful services.
* Aggregating Brokers consume all incoming messages until a condition is met ie aggregate every 10 messages.
* Re-Sequencing Brokers consume all incoming messages until a condition is met and then release them messages as a
* list of newly ordered events. This can provide a powerful function when combined with a Splitter as the next component.
*
* IMPORTANT: do not set the input type below to java.lang.Object. Ikasan decides whether this Broker wants just
* the payload or the full event by first trying invoke(FlowEvent) and catching a ClassCastException if that
* fails - java.lang.Object accepts anything without ever throwing, so it silently locks in "pass the whole
* FlowEvent" mode instead of the payload you're expecting. Leave the input type as the real payload type (e.g.
* String) to receive just the payload; set it to org.ikasan.spec.flow.FlowEvent instead if you need the full
* event (identifier, timestamp, etc. as well as the payload) - call payload.getPayload() inside invoke() to get
* the payload out.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be over-written unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.endpoint.Broker;
import org.ikasan.spec.component.endpoint.EndpointException;

@org.springframework.stereotype.Component
public class MyBroker implements Broker<java.lang.String, java.lang.Integer>
{

@Override
public java.lang.Integer invoke(java.lang.String payload) throws EndpointException
{
return java.lang.Integer.valueOf(payload);
}
}