package org.ikasan;

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.springframework.stereotype.Component;
import org.ikasan.studio.component.DebugTransitionComponent;

@org.springframework.stereotype.Component

public class AToBConvertMyFlow1SetByIDEEndingInDebugDebug extends org.ikasan.studio.component.DebugTransitionComponent
{
public void debug(java.lang.Object message)
{
System.out.println("Flow Debug Invoked");
}

}