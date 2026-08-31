package org.ikasan;

/**
* The main responsibility of a converter is to convert from one POJO type to another.
*
* Unlike Broker, this component detects "full event" mode safely: set the input type below to the real payload
* type (e.g. String) to receive just the payload, or to org.ikasan.spec.flow.FlowEvent to receive the full event
* (call payload.getPayload() inside convert() to get the payload out) - Ikasan checks your class for a literal
* convert(FlowEvent) method rather than catching a runtime cast failure, so java.lang.Object is not a trap here.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be overwritten unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component("org.ikasan.MyConverter")
public class MyConverter implements Converter<java.lang.String, java.lang.Integer>
{
public java.lang.Integer convert(java.lang.String payload) throws TransformationException
{
return new java.lang.Integer(payload);
}
}