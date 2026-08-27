package ${studioPackageTag};

/**
* Filtering rule (algorithm) for org.ikasan.filter.DefaultMessageFilter, which wraps an instance of this class
* and delegates each message to it - accept() decides whether the message continues through the flow (true)
* or is discarded (false), so unlike a plain Filter implementation there's no need to hand back a (possibly
* modified) message here.
*
* Note: accept() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
* there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
* or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements org.ikasan.spec.component.filter.FilterRule<${flowElement.getPropertyValue("fromType")}><#if flowElement.getPropertyValue("configuredResource")?has_content && flowElement.getPropertyValue("configuredResource")>,org.ikasan.spec.configuration.ConfiguredResource<${flowElement.getPropertyValue("configuration")}><#elseif flowElement.getPropertyValue("configuration")?has_content>, org.ikasan.spec.configuration.Configured<${flowElement.getPropertyValue("configuration")}></#if>
{
<#if flowElement.getPropertyValue("configuration")??>
${flowElement.getPropertyValue("configuration")} configuration;
</#if>
<#if flowElement.getPropertyValue("configuredResource")?has_content && flowElement.getPropertyValue("configuredResource")>
String configurationId;
</#if>
/**
* Evaluate the message against this rule.
*
* @param message
* @return true if the message is accepted (passed through), false if it should be filtered out.
* @throws org.ikasan.spec.component.filter.FilterException
*/
public boolean accept(${flowElement.getPropertyValue("fromType")} message) throws org.ikasan.spec.component.filter.FilterException
{
//@TODO implement your filter rule logic
return true;
}
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
