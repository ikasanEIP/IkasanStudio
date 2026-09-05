package ${studioPackageTag};

/**
* Consumers provide the 'glue' between the entry into the flow and the underlying technology generating the
* event e.g. polling a queue, listening on a socket, watching a directory.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("${studioPackageTag}.${flowElement.getPropertyValue("userImplementedClassName")}")

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<org.ikasan.spec.event.EventListener, org.ikasan.spec.event.EventFactory>
{
/** the listener notified of each event this consumer produces */
protected org.ikasan.spec.event.EventListener eventListener;

/** the factory used to wrap incoming data into a flow event */
protected org.ikasan.spec.event.EventFactory eventFactory;

private boolean running = false;

@Override
public void setListener(org.ikasan.spec.event.EventListener eventListener)
{
this.eventListener = eventListener;
}

@Override
public void setEventFactory(org.ikasan.spec.event.EventFactory eventFactory)
{
this.eventFactory = eventFactory;
}

@Override
public org.ikasan.spec.event.EventFactory getEventFactory()
{
return eventFactory;
}

/**
* Start the consumer and any underlying technology e.g. begin polling, open a listening socket.
*/
@Override
public void start()
{
//@TODO implement your consumer logic here e.g. start polling/listening on your underlying technology, and
// call eventListener.invoke(eventFactory.newEvent(identifier, payload)) for each event received.
running = true;
}

@Override
public boolean isRunning()
{
return running;
}

/**
* Stop the consumer and any underlying technology, releasing any resources acquired in start().
*/
@Override
public void stop()
{
//@TODO stop polling/listening and release any underlying resources.
running = false;
}
}