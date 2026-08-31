package ${studioPackageTag};

/**
* Base interface for filtering messages.
*
* Note: filter() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* Do not mutate the payload inside filter() below. Nothing in the flow defends against it - the exact same
* object reference you're given is what gets passed on to the next component if you return it - but a filter
* is meant to be a pass/fail gate, not a place to transform the payload's content or type. Use a Converter or
* Translator for that instead.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("${studioPackageTag}.${flowElement.getPropertyValue("userImplementedClassName")}")

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<${flowElement.getPropertyValue("fromType")}><#if flowElement.getPropertyValue("isConfiguredResource")?has_content && flowElement.getPropertyValue("isConfiguredResource")>,org.ikasan.spec.configuration.ConfiguredResource<${flowElement.getPropertyValue("cConfiguration")}><#elseif flowElement.getPropertyValue("configuration")?has_content>, org.ikasan.spec.configuration.Configured<${flowElement.getPropertyValue("configuration")}></#if>
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
* If the message does not match the criteria, return null - flow processing for this event simply stops
* here (there is no separate "discarded" destination it gets routed to); the invocation is recorded with a
* FILTER final action, and optionally logged, depending on this component's own logFiltered setting.
*
* @param message
* @return the (unmodified) message to let it continue through the flow, or null to stop processing it here.
* @throws FilterException
*/
public ${flowElement.getPropertyValue("fromType")} filter(${flowElement.getPropertyValue("fromType")} message) throws org.ikasan.spec.component.filter.FilterException
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
