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
<#--public class ${component.getPropertyValue("BespokeClassName")} implements Converter<${component.getPropertyValue("FromType")}, ${component.getPropertyValue("ToType")}>-->
public class ${component.getPropertyValue("BespokeClassName")} implements Converter<${component.getPropertyValue("FromType")}, ${component.getPropertyValue("ToType")}>
{
public ${component.getPropertyValue("ToType")} convert(${component.getPropertyValue("FromType")} payload) throws TransformationException
{
return ${component.getPropertyValue("ToType")}.valueOf(payload);
}
}