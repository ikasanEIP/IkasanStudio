<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component
public class ${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))} implements Converter<${flowElement.getPropertyValue('fromType')}, ${flowElement.getPropertyValue('toType')}>
{
public ${flowElement.getPropertyValue('toType')} convert(${flowElement.getPropertyValue('fromType')} payload) throws TransformationException
{
System.out.println("Flow Debug Invoked");
return ${flowElement.getPropertyValue('toType')}.valueOf(payload);
}
}