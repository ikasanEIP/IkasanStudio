package ${studioPackageTag};

/**
* Consumers provide the 'glue' between the entry into the flow and the underlying technology generating the
* event e.g. polling a queue, listening on a socket, watching a directory.
*
* Before start() is called, the framework normally wires this consumer up by calling setListener(...) and
* setEventFactory(...) below with the real Flow and a real EventFactory, so eventListener and eventFactory are
* both guaranteed to be set by the time your consumer logic runs. Until that happens, the fields below default
* to the small sample implementations at the bottom of this class, purely so the generated code compiles and
* does something sensible on its own.
*
* start() itself should return quickly - it just kicks off whatever generates events (a thread, a scheduled
* task, a listening socket) rather than doing the work inline. This stub ships with a runnable default: a
* poller that manufactures a dummy event every minute and dispatches it, purely so you can see the
* eventFactory -> eventListener handoff working end to end before replacing it with your real technology.
*
* @author Ikasan Development Team
*
*/

@org.springframework.stereotype.Component("${studioPackageTag}.${flowElement.getPropertyValue("userImplementedClassName")}")

public class ${flowElement.getPropertyValue("userImplementedClassName")} implements ${flowElement.getComponentMeta().getComponentType()}<org.ikasan.spec.event.EventListener, org.ikasan.spec.event.EventFactory>
{
private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(${flowElement.getPropertyValue("userImplementedClassName")}.class);

/** The thing to invoke with each event this consumer produces. In a real deployment the framework replaces
* this (via setListener(...) below) with the owning Flow - this defaults to SampleEventListener, defined at
* the bottom of this class, purely so the demo poller below has something real to call. Calling
* eventListener.invoke(event) is what actually pushes an event into the flow for processing; there are also
* invoke(Throwable) and invoke(Resubmission) overloads for reporting an error or replaying a prior event. */
protected org.ikasan.spec.event.EventListener eventListener = new SampleEventListener();

/** Builds the flow event from a raw payload coming from the underlying technology - it does not invoke
* anything itself. In a real deployment the framework replaces this (via setEventFactory(...) below) with the
* real EventFactory - this defaults to SampleEventFactory, defined at the bottom of this class, purely so the
* demo poller below has something real to call. Call eventFactory.newEvent(identifier, payload) to create the
* event, then pass the result to eventListener.invoke(...) below to actually dispatch it. */
protected org.ikasan.spec.event.EventFactory eventFactory = new SampleEventFactory();

/** Optional - present only if this project has a platform/JTA transaction manager bean configured (see
* IkasanTransactionConfiguration in ikasan-transaction-arjuna). A transactional downstream endpoint (FTP,
* SFTP, JMS, a DB producer) requires an active transaction on the thread that invokes it. A real Quartz-driven
* consumer gets one for free via Ikasan's built-in AOP advice on MessageListener.onMessage(...) - but that
* advice can't apply to a plain background thread like the poller below, since it only wraps calls made
* through a Spring-managed proxy. So the demo poller demarcates the transaction explicitly instead, with
* Spring's TransactionTemplate, and simply skips it if no transaction manager bean is present. */
@org.springframework.beans.factory.annotation.Autowired(required = false)
private org.springframework.transaction.PlatformTransactionManager transactionManager;

private boolean running = false;

//@TODO this poller is the runnable default described above - delete it, along with poll() below, once you
// replace start()/stop() with your real underlying technology (e.g. a JMS listener, an FTP poll, a file watch).
// Not created until start() - a ScheduledExecutorService cannot be restarted once stop() shuts it down, and a
// flow's consumer bean is a long-lived singleton that can genuinely be stopped and started again on the same
// instance, so a fresh one must be created each time start() runs rather than once at construction.
private java.util.concurrent.ScheduledExecutorService poller;
private long eventCount = 0;

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
//@TODO replace this scheduling with your real consumer logic e.g. open a connection, register a listener, or
// schedule your own poll - the important thing is that start() kicks work off and returns, it should not
// block. See poll() below for where each individual event actually gets built and dispatched.
poller = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
poller.scheduleWithFixedDelay(this::poll, 1, 1, java.util.concurrent.TimeUnit.MINUTES);
running = true;
}

/**
* Runnable default: manufactures a dummy event every tick so you can see events flow before wiring up your
* real technology. This is where the real work happens once you're polling/listening for real:
*   1. Obtain/assign an identifier for the message as a String (used for tracking and audit) - Ikasan's flow
*      event identifier is typed as String throughout its recovery/exclusion/error-reporting machinery, so a
*      non-String identifier compiles fine here but throws a ClassCastException later, deep inside that
*      machinery, only once something actually goes wrong with an event - not at the point you set it.
*   2. Wrap it into a flow event:  Object event = eventFactory.newEvent(identifier, payload);
*   3. Dispatch it into the flow, inside a transaction if one is available - see dispatch() below.
* If reading from the underlying technology fails, report the failure the same way: eventListener.invoke(throwable);
*/
private void poll()
{
try
{
eventCount++;
String identifier = "event-" + eventCount;
// Uncomment for diagnostics; log identifiers rather than message contents.
// LOG.debug("Dispatching event {}", identifier);
Object event = eventFactory.newEvent(identifier, "Hello from ${flowElement.getPropertyValue("userImplementedClassName")}, event #" + eventCount);
dispatch(event);
}
catch (Throwable throwable)
{
eventListener.invoke(throwable);
}
}

/**
* Dispatches a single event into the flow, wrapping it in a transaction when a transaction manager bean is
* available (see the transactionManager field above) - required for a downstream FTP/SFTP/JMS/DB endpoint to
* work, harmless otherwise. Without this, such an endpoint fails with a NullPointerException or similar deep
* inside its own connection handling, since it expects a transaction to already be active on this thread.
*/
private void dispatch(Object event)
{
if (transactionManager == null)
{
eventListener.invoke(event);
return;
}
new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(
new org.springframework.transaction.support.TransactionCallbackWithoutResult()
{
@Override
protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status)
{
eventListener.invoke(event);
}
}
);
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
//@TODO also release any real underlying resources here e.g. close a connection/socket.
if (poller != null)
{
poller.shutdownNow();
}
running = false;
}

/**
* Minimal, runnable stand-in for EventListener so this class does something sensible without any other
* wiring. In a real deployment this is normally a separate, external class - typically the framework
* substitutes the owning Flow itself here via setListener(...) above. Delete this once you no longer need
* the demo.
*/
private static class SampleEventListener implements org.ikasan.spec.event.EventListener
{
@Override
public void invoke(Object event)
{
// LOG.debug("Sample listener received an event");
}

@Override
public void invoke(Throwable throwable)
{
// LOG.error("Sample listener received an error", throwable);
}

@Override
public void invoke(org.ikasan.spec.event.Resubmission event)
{
// LOG.debug("Sample listener received a resubmission");
}
}

/**
* Minimal, runnable stand-in for EventFactory so this class does something sensible without any other
* wiring. In a real deployment this is normally a separate, external class supplied by the framework via
* setEventFactory(...) above, wrapping identifier/payload into whatever FlowEvent type the rest of the flow
* expects. Delete this once you no longer need the demo.
*/
private static class SampleEventFactory implements org.ikasan.spec.event.EventFactory
{
@Override
public Object newEvent(Object identifier, Object payload)
{
return "[" + identifier + "] " + payload;
}

@Override
public Object newEvent(Object identifier, Object relatedIdentifier, Object payload)
{
return "[" + identifier + " related-to " + relatedIdentifier + "] " + payload;
}

@Override
public Object newEvent(Object identifier, Object relatedIdentifier, long timestamp, Object payload)
{
return "[" + identifier + " related-to " + relatedIdentifier + " @" + timestamp + "] " + payload;
}
}
}
