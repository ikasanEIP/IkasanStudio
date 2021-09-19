package ${studioPackageTag};

/**
* Base interface for filtering messages.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class ${component.getPropertyValue("BespokeClassName")} implements org.ikasan.spec.component.filter.Filter<${component.getPropertyValue("FromType")}><#if component.getPropertyValue("IsEditable")?has_content && component.getPropertyValue("IsEditable")>, org.ikasan.spec.configuration.ConfiguredResource<${component.getPropertyValue("Configuration")}><#elseif component.getPropertyValue("Configuration")?has_content>, org.ikasan.spec.configuration.Configured<${component.getPropertyValue("Configuration")}></#if>
{
<#if component.getPropertyValue("Configuration")??>
${component.getPropertyValue("Configuration")} configuration;
</#if>
<#if component.getPropertyValue("IsEditable")?has_content && component.getPropertyValue("IsEditable")>
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
public ${component.getPropertyValue("FromType")} filter(${component.getPropertyValue("FromType")} message) throws org.ikasan.spec.component.filter.FilterException
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
<#if component.getPropertyValue("IsEditable")?has_content && component.getPropertyValue("IsEditable")>

@Override
public String getConfiguredResourceId() {
return configurationId;
}

@Override
public void setConfiguredResourceId(String id) {
this.configurationId = id;
}
</#if>
<#if component.getPropertyValue("Configuration")??>

@Override
public ${component.getPropertyValue("Configuration")} getConfiguration() {
return configuration;
}

@Override
public void setConfiguration(${component.getPropertyValue("Configuration")} configuration) {
this.configuration = configuration;
}
</#if>
}
