package org.ikasan;

/**
* Base interface for filtering messages.
*
* Note: filter() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class myFilter implements org.ikasan.spec.component.filter.Filter<java.lang.String>, org.ikasan.spec.configuration.Configured<MyConfigurationClass>
{
MyConfigurationClass configuration;
/**
* If the message matches the criteria specified by the MessageFilter implementation,
* the message is returned (passed through) and in turn routed to next part of the flow.
* If the message does not match the criteria, return null; route the message to a discarded
* message channel.
*
* @param message
* @return Message or null.
* @throws FilterException
*/
public java.lang.String filter(java.lang.String message) throws org.ikasan.spec.component.filter.FilterException
{
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
return message;
}
else {
//@TODO return null if your filter has filtered this message, maybe log this result.
return null;
}
}

@Override
public MyConfigurationClass getConfiguration() {
return configuration;
}

@Override
public void setConfiguration(MyConfigurationClass configuration) {
this.configuration = configuration;
}
}
