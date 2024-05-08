package org.ikasan;

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;
import org.springframework.stereotype.Component;

@org.springframework.stereotype.Component

public class AToBConvertMyFlow1SetByIDEEndingInDebugDebug implements Filter<java.lang.Object>
{
public java.lang.Object filter(java.lang.Object message) throws org.ikasan.spec.component.filter.FilterException
{
System.out.println("Flow Debug Invoked");
return message;
}

}