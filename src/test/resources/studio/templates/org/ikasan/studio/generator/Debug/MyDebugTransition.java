package org.ikasan;

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component

public class AToBConvertMyFlow1SetByIDEEndingInDebugDebug implements Converter<java.lang.Object, java.lang.Object>
{
public java.lang.Object convert(java.lang.Object payload) throws TransformationException
{
System.out.println("Flow Debug Invoked");
return payload;
}
}