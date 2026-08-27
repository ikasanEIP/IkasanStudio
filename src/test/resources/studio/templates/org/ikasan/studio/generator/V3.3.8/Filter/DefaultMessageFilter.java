package org.ikasan;

/**
* Filtering rule (algorithm) for org.ikasan.filter.DefaultMessageFilter, which wraps an instance of this class
* and delegates each message to it - accept() decides whether the message continues through the flow (true)
* or is discarded (false), so unlike a plain Filter implementation there's no need to hand back a (possibly
* modified) message here.
*
* Note: accept() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class myFilter implements org.ikasan.spec.component.filter.FilterRule<java.lang.String>,org.ikasan.spec.configuration.ConfiguredResource<MyConfigurationClass>
{
MyConfigurationClass configuration;
String configurationId;
/**
* Evaluate the message against this rule.
*
* @param message
* @return true if the message is accepted (passed through), false if it should be filtered out.
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
