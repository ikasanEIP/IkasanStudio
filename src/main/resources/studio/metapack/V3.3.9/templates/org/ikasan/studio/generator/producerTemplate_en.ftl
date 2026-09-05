package ${studioPackageTag};

/**
* Producers push a payload to a protocol endpoint, typically the last component of a flow or of a route
* within it.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("${studioPackageTag}.${flowElement.getPropertyValue("userImplementedClassName")}")

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<${flowElement.getPropertyValue("fromType")}>
{
/**
* Push the payload to your protocol endpoint.
*
* @param payload
* @throws org.ikasan.spec.component.endpoint.EndpointException
*/
@Override
public void invoke(${flowElement.getPropertyValue("fromType")} payload) throws org.ikasan.spec.component.endpoint.EndpointException
{
//@TODO implement your producer logic here e.g. push the payload to your protocol endpoint
}
}