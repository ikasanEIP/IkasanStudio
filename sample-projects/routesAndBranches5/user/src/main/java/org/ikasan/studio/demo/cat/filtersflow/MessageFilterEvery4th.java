package org.ikasan.studio.demo.cat.filtersflow;

import jakarta.jms.Message;
import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;
import org.springframework.stereotype.Component;

/**
 * Base interface for filtering messages.
 * <p>
 * Note: filter() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
 * there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
 * or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
 * <p>
 * Do not mutate the payload inside filter() below. Nothing in the flow defends against it - the exact same
 * object reference you're given is what gets passed on to the next component if you return it - but a filter
 * is meant to be a pass/fail gate, not a place to transform the payload's content or type. Use a Converter or
 * Translator for that instead.
 *
 * @author Ikasan Development Team
 *
 */

@Component("org.ikasan.studio.demo.cat.filtersflow.MessageFilterEvery4th")

public class MessageFilterEvery4th implements Filter<String> {
    /**
     * If the payload matches the criteria specified by the MessageFilter implementation,
     * the payload is returned (passed through) and in turn routed to next part of the flow.
     * If the payload does not match the criteria, return null - flow processing for this event simply stops
     * here (there is no separate "discarded" destination it gets routed to); the invocation is recorded with a
     * FILTER final action, and optionally logged, depending on this component's own logFiltered setting.
     *
     * @param message - the incoming payload
     * @return the (unmodified) payload to let it continue through the flow, or null to stop processing it here.
     * @throws FilterException if the filtering decision cannot be evaluated, for example because
     *                         required payload data is invalid or a lookup fails; normal rejection returns null
     */
    public String filter(String message) throws FilterException {
        if (message != null) {
            String[] splt = message.split("=");
            if (splt.length > 1) {
                int numeric = Integer.parseInt(splt[1]);
                return numeric % 4 == 0 ? null : message;
            }
        }
        System.out.println("Bob");
        return message;
    }
}
