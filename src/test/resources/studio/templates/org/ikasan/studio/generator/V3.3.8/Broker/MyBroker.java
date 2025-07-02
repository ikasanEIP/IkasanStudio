package org.ikasan;

/**
* Brokers enrich the contents of the existing message with additional data or structure in a number of different ways.
* Request Response Brokers can make calls to other systems such as a database or HTTP(s) RESTful services.
* Aggregating Brokers consume all incoming messages until a condition is met ie aggregate every 10 messages.
* Re-Sequencing Brokers consume all incoming messages until a condition is met and then release them messages as a
* list of newly ordered events. This can provide a powerful function when combined with a Splitter as the next component.
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