package org.ikasan;

/**
* The main responsibility of a converter is to convert from one POJO type to another.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be overwritten unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component
public class MyConverter implements Converter<java.lang.String, java.lang.Integer>
{
public java.lang.Integer convert(java.lang.String payload) throws TransformationException
{
return new java.lang.Integer(payload);
}
}