package ${studioPackageTag};

/**
* Base interface for filtering messages.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements org.ikasan.spec.flowElement.filter.Filter<${flowElement.getPropertyValue("fromType")}><#if flowElement.getPropertyValue("isConfiguredResource")?has_content && flowElement.getPropertyValue("isConfiguredResource")>, org.ikasan.spec.configuration.ConfiguredResource<${flowElement.getPropertyValue("cConfiguration")}><#elseif flowElement.getPropertyValue("configuration")?has_content>, org.ikasan.spec.configuration.Configured<${flowElement.getPropertyValue("configuration")}></#if>
{
<#if flowElement.getPropertyValue("configuration")??>
${flowElement.getPropertyValue("configuration")} configuration;
</#if>
<#if flowElement.getPropertyValue("isConfiguredResource")?has_content && flowElement.getPropertyValue("isConfiguredResource")>
String configurationId;
</#if>
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
public ${flowElement.getPropertyValue("fromType")} filter(${flowElement.getPropertyValue("fromType")} message) throws org.ikasan.spec.flowElement.filter.FilterException
{
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
return message;
}
else {
//@TODO return null if your filter has filtered this message, maybe log this result.
return null;
}
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
