package org.ikasan;

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class myMultiRecipientRouter implements org.ikasan.spec.component.routing.MultiRecipientRouter<java.lang.String>,    org.ikasan.spec.configuration.Configured<MyConfigurationClass>
{
MyConfigurationClass configuration;
/**
* The router needs to return the list of names (strings) of the routes this payload must be passed to
*
* @param payload to be evaluated and passed to the router routes
* @return A list of routerNames that payload will be passed to
*/
public java.lang.String route(java.lang.String payload)
{
List<String>routes = new ArrayList();
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
routes.add("firstRoute");
}
else {
routes.add("secondRoute");
}
return routes;
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
