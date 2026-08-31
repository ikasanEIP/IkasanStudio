<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
 * The main responsibility of a translator is to translate the payload (not the type).
 *
 * Note: translate() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
 * there is no full-event mode to opt into here. If your translation logic needs the event's identifier,
 * timestamp or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a
 * better fit.
 *
 * This is an auto generated stub. The user is expected to fill in the details of the conversion below.
 * This stub will not be over-written unless the overwrite checkbox is explicitly selected.
 */

import org.ikasan.spec.component.transformation.Translator;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component("${studioPackageTag}.${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))}")
public class ${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))} implements Translator<${flowElement.getPropertyValue('type')}>
{
/**
* Transforms or otherwise changes the passed in <code>Event</code>
*
* @param payload Event to transform
* @throws TransformationException Exception if we could not transform
*/
public void translate(${flowElement.getPropertyValue('type')} payload) throws TransformationException
{
System.out.println("Payload was" + payload);
}
}