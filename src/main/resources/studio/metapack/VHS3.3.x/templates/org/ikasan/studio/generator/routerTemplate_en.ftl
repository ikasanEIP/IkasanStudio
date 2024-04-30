package ${studioPackageTag};

/**
* User Implemented Class for routing payloads.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<${flowElement.getPropertyValue("fromType")}><#if flowElement.getPropertyValue("isConfiguredResource")?has_content&&flowElement.getPropertyValue("isConfiguredResource")>, org.ikasan.spec.configuration.ConfiguredResource<    ${flowElement.getPropertyValue("cConfiguration")}><#elseif flowElement.getPropertyValue("configuration")?has_content>,    org.ikasan.spec.configuration.Configured<${flowElement.getPropertyValue("configuration")}></#if>
{
<#if flowElement.getPropertyValue("configuration")??>
${flowElement.getPropertyValue("configuration")} configuration;
</#if>
<#if flowElement.getPropertyValue("isConfiguredResource")?has_content && flowElement.getPropertyValue("isConfiguredResource")>
String configurationId;
</#if>

<#list flowElement.getPropertyValue("routeNames")![] as route>
public static final String ${route?upper_case} = "${route}";
</#list>

/**
* ${flowElement.getComponentMeta().getHelpText()}
*
* @param payload to be evaluated and passed to the router routes
* @return A list of routerNames that payload will be passed to
*/
@Override
public ${flowElement.getPropertyValue("toType")} route(${flowElement.getPropertyValue("fromType")} payload) throws org.ikasan.spec.component.routing.RouterException
{
//List<String>routes = new ArrayList();
//if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
//routes.add("firstRoute");
//}
//else {
//routes.add("secondRoute");
//}
return null;
}
<#if flowElement.getPropertyValue("isConfiguredResource")?has_content && flowElement.getPropertyValue("isConfiguredResource")>

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