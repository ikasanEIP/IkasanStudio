package ${studioPackageTag};

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<${flowElement.getPropertyValue("fromType")}><#if flowElement.getPropertyValue("configuredResource")?has_content && flowElement.getPropertyValue("configuredResource")>, org.ikasan.spec.configuration.ConfiguredResource<    ${flowElement.getPropertyValue("configuration")}><#elseif flowElement.getPropertyValue("configuration")?has_content>,    org.ikasan.spec.configuration.Configured<${flowElement.getPropertyValue("configuration")}></#if>
{
<#if flowElement.getPropertyValue("configuration")??>
${flowElement.getPropertyValue("configuration")} configuration;
</#if>
<#if flowElement.getPropertyValue("configuredResource")?has_content && flowElement.getPropertyValue("configuredResource")>
String configurationId;
</#if>

<#list flowElement.getPropertyValue("routeNames")![] as route>
public static final String ${route?upper_case} = "${route}";
</#list>

<#if flowElement.getComponentMeta().getName() == "Multi Recipient Router">
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
public ${flowElement.getPropertyValue("toType")} route(${flowElement.getPropertyValue("fromType")} payload) throws org.ikasan.spec.component.routing.RouterException
{
//@TODO implement your routing logic, returning the route name(s) (see the constants above) payload should be sent to
return java.util.List.of(<#list flowElement.getPropertyValue("routeNames")![] as route>${route?upper_case}<#sep>, </#sep></#list>);
}
<#else>
/**
* ${flowElement.getComponentMeta().getHelpText()}
*
* @param payload to be evaluated to choose exactly one of the routes above (see the constants above) for payload to take next
* @return the single route name (see the constants above) payload should be sent to. Returning null routes to
* a "default" transition if (and only if) one has been configured below - if not, or if the returned name
* matches none of this router's own routes, the flow throws an InvalidFlowException at runtime (see
* org.ikasan.flow.visitorPattern.invoker.SingleRecipientRouterFlowElementInvoker), so only rely on null if a
* "default" route genuinely exists among the route names above.
*/
@Override
public ${flowElement.getPropertyValue("toType")} route(${flowElement.getPropertyValue("fromType")} payload) throws org.ikasan.spec.component.routing.RouterException
{
//@TODO implement your routing logic, returning exactly one of the route names (see the constants above) payload should be sent to
return ${(flowElement.getPropertyValue("routeNames")![])[0]?upper_case};
}
</#if>
<#if flowElement.getPropertyValue("configuredResource")?has_content && flowElement.getPropertyValue("configuredResource")>

@Override
public String getConfiguredResourceId() {
return configurationId;
}

@Override
public void setConfiguredResourceId(String id) {
this.configurationId = id;
}
</#if>
<#if flowElement.getPropertyValue("configuration")??>

@Override
public ${flowElement.getPropertyValue("configuration")} getConfiguration() {
return configuration;
}

@Override
public void setConfiguration(${flowElement.getPropertyValue("configuration")} configuration) {
this.configuration = configuration;
}
</#if>
}