package org.example.rab.filtersflow;

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
 */

@Component("org.example.rab.filtersflow.NormalFilter")

public class NormalFilter implements Filter<String> {
    /**
     * If the message matches the criteria specified by the MessageFilter implementation,
     * the message is returned (passed through) and in turn routed to next part of the flow.
     * If the message does not match the criteria, return null - flow processing for this event simply stops
     * here (there is no separate "discarded" destination it gets routed to); the invocation is recorded with a
     * FILTER final action, and optionally logged, depending on this component's own logFiltered setting.
     *
     * @param message
     * @return the (unmodified) message to let it continue through the flow, or null to stop processing it here.
     * @throws FilterException
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
