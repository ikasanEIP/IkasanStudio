<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
* The main responsibility of a converter is to convert from one POJO type to another.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be overwritten unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component
public class ${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))} implements Converter<${flowElement.getPropertyValue('fromType')}, ${flowElement.getPropertyValue('toType')}>
{
public ${flowElement.getPropertyValue('toType')} convert(${flowElement.getPropertyValue('fromType')} payload) throws TransformationException
{
return new ${flowElement.getPropertyValue('toType')}(payload);
}
}