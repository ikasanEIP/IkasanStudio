package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups every component that can use Studio's embedded test FTP server (see
 * {@code ComponentMeta#supportsTestFtpServer()}) by the FTP address it is configured for, splitting each group
 * into the consumers reading FROM that server and the producers delivering TO it.
 * -
 * Fills the same role for the canvas's "Test FTP Server" node that {@link TestMailServerLinks} fills for the
 * mail server, with one extra dimension: a mail server is only ever downstream of an Email Producer, whereas an
 * FTP server can sit on either side of a flow. Consumers take their files from it (so its node belongs to the
 * LEFT of the flow, feeding in) and producers deliver to it (so its node belongs to the RIGHT, being fed). They
 * are kept apart here rather than merged into one node so the canvas can draw one node per side and never has
 * to run a connector line backwards from a right-hand node to a left-hand component.
 * -
 * Framework-independent (no PSI/UI dependency), like {@link TestMailServerLinks}, so the grouping itself stays
 * plain-JUnit testable - whether a server is actually listening at that address is a live question owned by the
 * ui/runtime layer (see {@code TestFtpServerService}).
 */
public final class TestFtpServerLinks {

    private TestFtpServerLinks() {}

    /**
     * One FTP address, and the components on each side of the flow configured to use it.
     * @param configuration the shared address (host/port, plus the credentials of whichever component was seen
     *                      first - see {@link TestFtpServerConfiguration#sameAddressAs} for why credentials do
     *                      not take part in the grouping).
     * @param consumers components reading from that address - drawn to the LEFT of their flow.
     * @param producers components delivering to that address - drawn to the RIGHT of their flow.
     */
    public record Link(TestFtpServerConfiguration configuration, List<FlowElement> consumers, List<FlowElement> producers) {
        public String address() {
            return configuration.address();
        }
    }

    /**
     * @param module to scan - the canvas only ever paints one Module at a time.
     * @return every test-FTP-capable component across every flow, grouped by the address it points at. Usually a
     * single Link (one shared local test server), but more than one if components are configured for different
     * hosts/ports. Empty if the module has no such component at all.
     */
    public static List<Link> findLinks(Module module) {
        List<Link> links = new ArrayList<>();
        if (module == null || module.getFlows() == null) {
            return links;
        }
        for (Flow flow : module.getFlows()) {
            if (flow == null) {
                continue;
            }
            List<FlowElement> elements = flow.ftlGetConsumerAndFlowElements();
            if (elements == null) {
                continue;
            }
            for (FlowElement element : elements) {
                if (element == null || element.getComponentMeta() == null
                        || !element.getComponentMeta().supportsTestFtpServer()) {
                    continue;
                }
                TestFtpServerConfiguration configuration = TestFtpServerConfiguration.from(element);
                Link link = findOrCreateLink(links, configuration);
                // Which side the node goes on is derived from the component's own type - no per-component
                // branching, and no extra metadata field, needed.
                if (element.getComponentMeta().isConsumer()) {
                    link.consumers().add(element);
                } else if (element.getComponentMeta().isProducer()) {
                    link.producers().add(element);
                }
            }
        }
        return links;
    }

    private static Link findOrCreateLink(List<Link> links, TestFtpServerConfiguration configuration) {
        for (Link existing : links) {
            if (existing.configuration().sameAddressAs(configuration)) {
                return existing;
            }
        }
        Link created = new Link(configuration, new ArrayList<>(), new ArrayList<>());
        links.add(created);
        return created;
    }
}
