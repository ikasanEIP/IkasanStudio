package org.example.cat4.filtersflow;

import org.ikasan.spec.component.filter.FilterException;
import org.ikasan.spec.component.filter.FilterRule;
import org.springframework.stereotype.Component;

/**
 * Filtering rule (algorithm) for org.ikasan.filter.DefaultMessageFilter, which wraps an instance of this class
 * and delegates each payload to it - accept() decides whether the payload continues through the flow (true)
 * or is discarded (false); unlike a plain Filter implementation there's no need to hand back the payload here,
 * since DefaultMessageFilter itself returns the original object unchanged whenever accept() returns true.
 * <p>
 * Note: accept() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
 * there is no full-event mode to opt into here. If your filter logic needs the event's identifier, timestamp
 * or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a better fit.
 * <p>
 * Do not mutate the payload inside accept() below. Nothing in the flow defends against it - the exact same
 * object reference you're given is what DefaultMessageFilter passes on to the next component when you return
 * true - but a filter rule is meant to be a pass/fail gate, not a place to transform the payload's content or
 * type. Use a Converter or Translator for that instead.
 *
 * @author Ikasan Development Team
 */

@Component("org.example.cat4.filtersflow.DefaultFilterEvery5th")

public class DefaultFilterEvery5th implements FilterRule<String> {
    /**
     * Evaluate the payload against this rule.
     *
     * @param message - the incoming payload
     * @return true if the payload is accepted (passed through, unmodified) - false to stop processing it here;
     * there is no separate "discarded" destination it gets routed to, flow processing for this event simply ends.
     * @throws FilterException if the rule cannot be evaluated, for example
     *                         because required payload data is invalid or a lookup fails; normal rejection returns false
     */
    public boolean accept(String message) throws FilterException {
        if (message != null) {
            String[] splt = message.split("=");
            if (splt.length > 1) {
                int numeric = Integer.parseInt(splt[1]);
                return numeric % 5 != 0;
            }
        }
        return true;
    }
}
