package org.ikasan;

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("org.ikasan.myMultiRecipientRouter")

public class myMultiRecipientRouter implements org.ikasan.spec.component.routing.MultiRecipientRouter<java.lang.String>, org.ikasan.spec.configuration.ConfiguredResource<    MyConfigurationClass>
{
MyConfigurationClass configuration;
String configurationId;

public static final String ROUTE1 = "route1";
public static final String ROUTE2 = "route2";

/**
* <strong>Multi Recipient Router</strong><p>The router will allow the payload to be sent conditionally to one or many
* routes e.g. the logic could send the payload to routeA AND routeB.</p><p>Unlike the Single Recipient Router there
* is no "default" fallback - the router must always return at least one route name, it can never return zero.</p>
* <p>Each matched route runs to completion, in order, before the next one starts. By default the payload is cloned
* before being sent down each route (except the last) so routes cannot affect each other by mutating a shared payload.</p>
*
* @param payload to be evaluated to choose which of the routes above (see the constants above) payload should be sent to
* @return the route names (see the constants above) payload should be sent to, in any combination - null or an
* empty list is not valid, at least one route is always required (there is no "default" fallback for this
* router type - see org.ikasan.flow.visitorPattern.invoker.MultiRecipientRouterFlowElementInvoker)
*/
@Override
public java.util.List<java.lang.String> route(java.lang.String payload) throws org.ikasan.spec.component.routing.RouterException
{
//@TODO implement your routing logic, returning the route name(s) (see the constants above) payload should be sent to
return java.util.List.of(ROUTE1, ROUTE2);
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