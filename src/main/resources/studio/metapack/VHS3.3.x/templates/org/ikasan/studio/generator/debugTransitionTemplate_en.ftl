<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;
import org.springframework.stereotype.Component;

@org.springframework.stereotype.Component

public class ${StudioBuildUtils.substitutePlaceholderInPascalCase(module, flow, flowElement, flowElement.getPropertyValue('userImplementedClassName'))} implements Filter<${flowElement.getPropertyValue('fromType')}>
{
public ${flowElement.getPropertyValue("fromType")} filter(${flowElement.getPropertyValue("fromType")} message) throws org.ikasan.spec.component.filter.FilterException
{
System.out.println("Flow Debug Invoked");
return message;
}

}