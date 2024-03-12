<#assign StudioUtils=statics['org.ikasan.studio.StudioUtils']>
package ${studioPackageTag};

/**
* The main responsibility of a converter is to convert from one POJO type to another.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be over-written unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component
public class ${StudioUtils.toPascalCase(flowElement.getPropertyValue('bespokeClassName'))} implements Converter<${flowElement.getPropertyValue('fromType')}, ${flowElement.getPropertyValue('toType')}>
{
public ${flowElement.getPropertyValue('toType')} convert(${flowElement.getPropertyValue('fromType')} payload) throws TransformationException
{
return ${flowElement.getPropertyValue('toType')}.valueOf(payload);
}
}