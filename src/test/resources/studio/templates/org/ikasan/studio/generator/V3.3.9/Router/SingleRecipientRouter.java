package org.ikasan;

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("org.ikasan.mySingleRecipientRouter")

public class mySingleRecipientRouter implements org.ikasan.spec.component.routing.SingleRecipientRouter<java.lang.String>, org.ikasan.spec.configuration.ConfiguredResource<    MyConfigurationClass>
{
MyConfigurationClass configuration;
String configurationId;

public static final String ROUTE1 = "route1";
public static final String ROUTE2 = "route2";

/**
* <p>Routers choose which downstream route or routes receive each payload.</p><p>Use this router to select at most one route, such as routeA or routeB, but not both.</p><p>Configure at least two route names; with only one, there is no choice to make.</p>
*
* @param payload to be evaluated to choose exactly one of the routes above (see the constants above) for payload to take next
* @return the single route name (see the constants above) payload should be sent to. Returning null routes to
* a "default" transition if (and only if) one has been configured below - if not, or if the returned name
* matches none of this router's own routes, the flow throws an InvalidFlowException at runtime (see
* org.ikasan.flow.visitorPattern.invoker.SingleRecipientRouterFlowElementInvoker), so only rely on null if a
* "default" route genuinely exists among the route names above.
*/
@Override
public java.lang.String route(java.lang.String payload) throws org.ikasan.spec.component.routing.RouterException
{
//@TODO implement your routing logic, returning exactly one of the route names (see the constants above) payload should be sent to
return ROUTE1;
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