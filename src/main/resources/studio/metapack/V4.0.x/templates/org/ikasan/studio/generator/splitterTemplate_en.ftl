<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
* ${flowElement.getComponentMeta().getHelpText()}
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be over-written unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.splitting.Splitter;
import org.ikasan.spec.component.splitting.SplitterException;

@org.springframework.stereotype.Component
public class ${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))} implements Splitter<${flowElement.getPropertyValue('fromType')}, ${flowElement.getPropertyValue('toType')}>
{
public java.util.List<${flowElement.getPropertyValue('toType')}> split(${flowElement.getPropertyValue('fromType')} payload) throws SplitterException
{
java.util.List<${flowElement.getPropertyValue('toType')}> returnPayload = null;
// Your code here to generate a list of objects for the downstream from the single input object
return returnPayload;
}
}