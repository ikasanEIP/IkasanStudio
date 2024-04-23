package org.ikasan;

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class mySingleRecipientRouter implements org.ikasan.spec.component.routing.SingleRecipientRouter<java.lang.String>,    org.ikasan.spec.configuration.Configured<MyConfigurationClass>
{
MyConfigurationClass configuration;

public static final String ROUTE1 = "route1";
public static final String ROUTE2 = "route2";

/**
* The router will allow the payload to be sent conditionally to zero or one routes.
*
* @param payload to be evaluated and passed to the router routes
* @return A list of routerNames that payload will be passed to
*/
@override
public java.lang.String route(java.lang.String payload) throws org.ikasan.spec.component.routing.RouterException
{
List<String>routes = new ArrayList();
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
routes.add("firstRoute");
}
else {
routes.add("secondRoute");
}
return route / routes;
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