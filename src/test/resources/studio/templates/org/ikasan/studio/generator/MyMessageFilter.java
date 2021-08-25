package org.ikasan;

/**
* Base interface for filtering messages.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class MyFilter implements org.ikasan.spec.component.filter.Filter<class java.lang.String>, org.ikasan.spec.configuration.ConfiguredResource<MyConfigurationClass>
{
MyConfigurationClass configuration;
String MyConfiguredResourceId;
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
public class java.lang.String filter(class java.lang.String message) throws org.ikasan.spec.component.filter.FilterException
{
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
return message;
}
else {
//@TODO return null if your filter has filtered this message
return null;
}
}

@Override
public String getConfiguredResourceId() {
return MyConfiguredResourceId;
}

@Override
public void setConfiguredResourceId(String id) {
this.MyConfiguredResourceId = id;
}

@Override
public Config1 getConfiguration() {
return configuration;
}

@Override
public void setConfiguration(Config1 configuration) {
this.configuration = configuration;
}
}
