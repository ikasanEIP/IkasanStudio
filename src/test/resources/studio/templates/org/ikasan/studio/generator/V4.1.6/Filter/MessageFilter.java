package org.ikasan;

/**
* Base interface for filtering messages.
*
* Note: filter() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* Do not mutate the payload inside filter() below. Nothing in the flow defends against it - the exact same
* object reference you're given is what gets passed on to the next component if you return it - but a filter
* is meant to be a pass/fail gate, not a place to transform the payload's content or type. Use a Converter or
* Translator for that instead.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("org.ikasan.myFilter")

public class myFilter implements org.ikasan.spec.component.filter.Filter<java.lang.String>, org.ikasan.spec.configuration.Configured<MyConfigurationClass>
{
MyConfigurationClass configuration;
/**
* If the payload matches the criteria specified by the MessageFilter implementation,
* the payload is returned (passed through) and in turn routed to next part of the flow.
* If the payload does not match the criteria, return null - flow processing for this event simply stops
* here (there is no separate "discarded" destination it gets routed to); the invocation is recorded with a
* FILTER final action, and optionally logged, depending on this component's own logFiltered setting.
*
* @param payload - the incoming payload
* @return the (unmodified) payload to let it continue through the flow, or null to stop processing it here.
* @throws FilterException if the filtering decision cannot be evaluated, for example because
* required payload data is invalid or a lookup fails; normal rejection returns null
*/
public java.lang.String filter(java.lang.String payload) throws org.ikasan.spec.component.filter.FilterException
{
if (true) {
//@TODO implement your filter logic, return the payload if it is allowed by your filter
return payload;
}
else {
//@TODO return null if your filter has filtered this payload, maybe log this result.
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
