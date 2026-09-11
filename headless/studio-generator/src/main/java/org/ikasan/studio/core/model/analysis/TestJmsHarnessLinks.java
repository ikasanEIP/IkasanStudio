package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Pairs each JMS test-consumer harness flow (created by {@code CreateTestJmsConsumerFlowAction}: a real
 * Consumer + Debug + Dev Null Producer sink flow, existing purely to let a developer inspect what a real
 * Producer sends) with the Producer it was created for, using the hidden {@code testHarnessOwner} property
 * every {@code Flow} carries (see Flow's own component-meta_en_GB.json). Framework-independent, like
 * {@link TestFtpServerLinks}/{@link TestMailServerLinks}, so the canvas can decide - purely from this - which
 * flows to hide from normal rendering and draw a compact harness node against their owner instead.
 */
public final class TestJmsHarnessLinks {

    public static final String TEST_DESTINATION = "testHarnessDestination";
    private static final String SEPARATOR = "/";

    private TestJmsHarnessLinks() {}

    /**
     * @param ownerProducer the real Producer this harness was created to inspect - the compact node is drawn
     *                       against this component's own endpoint, and it's what the "Remove test harness"
     *                       context menu action resolves back from a click.
     * @param harnessFlow the hidden Flow - never painted as a normal flow route once it has a Link here.
     * @param debugComponent the harness flow's own Debug component, or null if one was somehow never inserted -
     *                        double-clicking the harness node navigates to this component's generated code.
     */
    public record Link(FlowElement ownerProducer, Flow harnessFlow, FlowElement debugComponent) {}

    /**
     * The stable identifier stamped into a harness flow's hidden {@code testHarnessOwner} property, and parsed
     * back out by {@link #findLinks}. A flow identity and component identity are each unique only within their
     * own scope (a component name is unique within its flow, a flow name within its module - see BasicElement's
     * own javadoc) - joining them is what makes the pair unique across the whole module.
     */
    public static String ownerKeyFor(FlowElement producer) {
        return producer.getContainingFlow().getIdentity() + SEPARATOR + producer.getIdentity();
    }

    /**
     * @param module to scan - the canvas only ever paints one Module at a time.
     * @return one Link per harness flow found, skipping any whose owner producer or debug component can no
     * longer be resolved (e.g. the producer or debug element was since deleted/renamed) rather than guessing.
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
            Object ownerKeyValue = flow.getPropertyValue("testHarnessOwner");
            String ownerKey = ownerKeyValue == null ? null : ownerKeyValue.toString();
            if (ownerKey == null || ownerKey.isBlank()) {
                continue;
            }
            FlowElement owner = resolveOwner(module, ownerKey);
            if (owner == null) {
                continue;
            }
            links.add(new Link(owner, flow, findDebugComponent(flow)));
        }
        return links;
    }

    /** A persisted private destination; the producer's configured destination is never changed. */
    public static String newTestDestination(FlowElement producer) {
        String prefix = Boolean.TRUE.equals(producer.getPropertyValue("pubSubDomain"))
                ? "dynamicTopics/" : "dynamicQueues/";
        return prefix + "studio-test-" + java.util.UUID.randomUUID();
    }

    public static boolean isDivertedHarness(Flow flow) {
        Object destination = flow.getPropertyValue(TEST_DESTINATION);
        return destination != null && !destination.toString().isBlank();
    }

    /** Used by generated wiring and canvas links, never by persisted producer configuration. */
    public static String destinationOverride(Module module, FlowElement element) {
        for (Link link : findLinks(module)) {
            if (isDivertedHarness(link.harnessFlow())
                    && (link.ownerProducer() == element || link.harnessFlow().getConsumer() == element)) {
                return link.harnessFlow().getPropertyValue(TEST_DESTINATION).toString();
            }
        }
        return null;
    }

    private static FlowElement resolveOwner(Module module, String ownerKey) {
        int separatorIndex = ownerKey.indexOf(SEPARATOR);
        if (separatorIndex < 0) {
            return null;
        }
        String flowIdentity = ownerKey.substring(0, separatorIndex);
        String componentIdentity = ownerKey.substring(separatorIndex + 1);
        for (Flow flow : module.getFlows()) {
            if (flow == null || !flowIdentity.equals(flow.getIdentity())) {
                continue;
            }
            List<FlowElement> elements = flow.ftlGetConsumerAndFlowElements();
            if (elements == null) {
                continue;
            }
            for (FlowElement element : elements) {
                if (element != null && componentIdentity.equals(element.getIdentity())) {
                    return element;
                }
            }
        }
        return null;
    }

    private static FlowElement findDebugComponent(Flow harnessFlow) {
        List<FlowElement> elements = harnessFlow.ftlGetConsumerAndFlowElements();
        if (elements == null) {
            return null;
        }
        for (FlowElement element : elements) {
            if (element != null && element.getComponentMeta() != null && element.getComponentMeta().isDebug()) {
                return element;
            }
        }
        return null;
    }
}
