package org.ikasan;

/**
* Filtering rule (algorithm) for org.ikasan.filter.DefaultMessageFilter, which wraps an instance of this class
* and delegates each message to it - accept() decides whether the message continues through the flow (true)
* or is discarded (false); unlike a plain Filter implementation there's no need to hand back the message here,
* since DefaultMessageFilter itself returns the original object unchanged whenever accept() returns true.
*
* Note: accept() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* Do not mutate the payload inside accept() below. Nothing in the flow defends against it - the exact same
* object reference you're given is what DefaultMessageFilter passes on to the next component when you return
* true - but a filter rule is meant to be a pass/fail gate, not a place to transform the payload's content or
* type. Use a Converter or Translator for that instead.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("org.ikasan.myFilter")

public class myFilter implements org.ikasan.spec.component.filter.FilterRule<java.lang.String>,org.ikasan.spec.configuration.ConfiguredResource<MyConfigurationClass>
{
MyConfigurationClass configuration;
String configurationId;
/**
* Evaluate the message against this rule.
*
* @param message
* @return true if the message is accepted (passed through, unmodified) - false to stop processing it here;
* there is no separate "discarded" destination it gets routed to, flow processing for this event simply ends.
* @throws org.ikasan.spec.component.filter.FilterException
*/
public boolean accept(java.lang.String message) throws org.ikasan.spec.component.filter.FilterException
{
//@TODO implement your filter rule logic
return true;
}

@Override
public String getConfiguredResourceId() {
return configurationId;
}

@Override
public void setConfiguredResourceId(String id) {
this.configurationId = id;
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
