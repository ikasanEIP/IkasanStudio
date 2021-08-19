package org.ikasan;

/**
* Base interface for filtering messages.
*
* @author Ikasan Development Team
*
*/

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;

@org.springframework.stereotype.Component

public class MyMessageFilter implements Filter<java.lang.String>
{
/**
* If the message matches the criteria specified by the MessageFilter implementation,
* the message is returned (passed through) and in turn routed to next part of the flow.
* If the message does not match the criteria, return null; route the message to a discarded
* message channel.
*
* @param message
* @return Message or null.
* @throws FilterException
*/
public java.lang.String filter(java.lang.String message) throws FilterException
{
if (true) {
//@TODO implement your filter logic, return the message if it is allowed by your filter
return message;
}
else {
//@TODO return null if your filter has filtered this message
return null;
}
}
}
