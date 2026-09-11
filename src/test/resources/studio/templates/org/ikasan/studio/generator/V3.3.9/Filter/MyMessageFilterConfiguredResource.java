package org.ikasan;

/**
* Base interface for filtering messages.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("org.ikasan.MyMessageFilter")

public class MyMessageFilter implements org.ikasan.spec.component.filter.Filter<class java.lang.String>, org.ikasan.spec.configuration.ConfiguredResource<MyConfigurationClass>
{
MyConfigurationClass configuration;
String configurationId;
/**
* If the payload matches the criteria specified by the MessageFilter implementation,
* the payload is returned (passed through) and in turn routed to next part of the flow.
* If the payload does not match the criteria, return null; route the payload to a discarded
* payload channel.
*
* @param payload - the incoming payload
* @return Message or null.
* @throws FilterException if the filtering decision cannot be evaluated, for example because
* required payload data is invalid or a lookup fails; normal rejection returns null
*/
public class java.lang.String filter(class java.lang.String payload) throws org.ikasan.spec.component.filter.FilterException
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
