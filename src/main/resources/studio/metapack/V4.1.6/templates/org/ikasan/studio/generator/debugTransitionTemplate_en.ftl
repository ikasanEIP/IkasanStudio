<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
* The purpose of this component is to allow the developer to set breakpoints in between components and inspect the payload
*/


@org.springframework.stereotype.Component("${studioPackageTag}.${StudioBuildUtils.substitutePlaceholderInPascalCase(module, flow, flowElement, flowElement.getPropertyValue('userImplementedClassName'))}")

public class ${StudioBuildUtils.substitutePlaceholderInPascalCase(module, flow, flowElement, flowElement.getPropertyValue('userImplementedClassName'))} extends org.ikasan.studio.component.DebugTransitionComponent
{

/**
* Allow user to view the message payload.
* The framework attempts to copy the payload; copying is best-effort, so inspect it without mutation.
* @param payload the payload or its best-effort copy; may be null
*/
@Override
public void debug(java.lang.Object payload)
{
// Add breakpoints here
org.slf4j.LoggerFactory.getLogger(getClass()).debug("Flow debug invoked for payload type {}",
        payload == null ? "null" : payload.getClass().getName());
}

}