package org.ikasan.studio.demo.cat.routerflow;

import org.ikasan.spec.component.routing.RouterException;
import org.ikasan.spec.component.routing.SingleRecipientRouter;
import org.springframework.stereotype.Component;

/**
 * User Implemented Class for routing payloads.
 *
 * @author Ikasan Development Team
 *
 */

@Component("org.ikasan.studio.demo.cat.routerflow.EitherOrRouter")

public class EitherOrRouter implements SingleRecipientRouter<String> {

    public static final String ODD = "odd";
    public static final String EVEN = "even";

    /**
     * <p>Routers choose which downstream route or routes receive each payload.</p><p>Use this router to select at most one route, such as routeA or routeB, but not both.</p><p>Configure at least two route names; with only one, there is no choice to make.</p>
     *
     * @param payload to be evaluated to choose exactly one of the routes above (see the constants above) for payload to take next
     * @return the single route name (see the constants above) payload should be sent to. Returning null routes to
     * a "default" transition if (and only if) one has been configured below - if not, or if the returned name
     * matches none of this router's own routes, the flow throws an InvalidFlowException at runtime (see
     * org.ikasan.flow.visitorPattern.invoker.SingleRecipientRouterFlowElementInvoker), so only rely on null if a
     * "default" route genuinely exists among the route names above.
     * @throws RouterException if the routing decision cannot be evaluated from the payload
     */
    @Override
    public String route(String payload) throws RouterException {
        if (payload != null) {
            String[] splt = payload.split("=");
            if (splt.length > 1) {
                int numeric = Integer.parseInt(splt[1]);
                return numeric % 2 == 0 ? EVEN : ODD;
            }
        }
        return ODD;
    }
}