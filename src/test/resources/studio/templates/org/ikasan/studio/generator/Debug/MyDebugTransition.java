package org.ikasan;

/**
* The purpose of this component is to allow the developer to set breakpoints in between components and inspect the payload
*/

import org.springframework.stereotype.Component;
import org.ikasan.studio.component.DebugTransitionComponent;

@org.springframework.stereotype.Component

public class AToBConvertMyFlow1SetByIDEEndingInDebugDebug extends org.ikasan.studio.component.DebugTransitionComponent
{

/**
* Allow use to view a clone of the message payload.
* This component will have no effect on the payload, and it will not alter the flow of any messages
* @param clonedMessage is a copy of the payload.
*/
public void debug(java.lang.Object clonedMessage)
{
// Add breakpoints here
System.out.println("Flow Debug Invoked " + clonedMessage.toString());
}

}