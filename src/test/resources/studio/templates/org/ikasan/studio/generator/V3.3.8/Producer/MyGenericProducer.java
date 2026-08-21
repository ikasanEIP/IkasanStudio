package org.ikasan;

/**
* Producers push a payload to a protocol endpoint, typically the last component of a flow or of a route
* within it.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component

public class myGenericProducer implements org.ikasan.spec.component.endpoint.Producer<java.lang.String>
{
/**
* Push the payload to your protocol endpoint.
*
* @param payload
* @throws org.ikasan.spec.component.endpoint.EndpointException
*/
@Override
public void invoke(java.lang.String payload) throws org.ikasan.spec.component.endpoint.EndpointException
{
//@TODO implement your producer logic here e.g. push the payload to your protocol endpoint
}
}