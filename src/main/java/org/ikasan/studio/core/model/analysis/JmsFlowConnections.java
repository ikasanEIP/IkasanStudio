package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finds pairs of JMS Producer/Consumer components, in different Flows of the same Module, that reference the
 * same JMS destination - so the canvas can draw an ESB-style connector line between them (see
 * {@code DesignerCanvas#paintJmsDestinationConnectors}), without the user needing to build a second consuming
 * flow just to trace the relationship visually.
 * -
 * Framework-independent (no PSI/UI dependency), like {@link org.ikasan.studio.core.model.command.FlowElementMove}, so it stays plain-JUnit testable.
 */
public final class JmsFlowConnections {
    // Every JMS component (JmsProducer, BasicAmqJmsProducer, SpringJmsConsumer, BasicAmqSpringJmsConsumer, in
    // both V3.3.8 and V4.0.x) carries this endpointKey, and nothing else does - the cleanest generic "is this
    // component JMS" discriminator, cheaper and more future-proof than an implementingClass allow-list.
    private static final String JMS_ENDPOINT_KEY = "Channel Endpoint";

    private static final String DESTINATION_JNDI_NAME = "destinationJndiName";
    private static final String CONNECTION_FACTORY_NAME = "connectionFactoryName";
    private static final String CONNECTION_FACTORY_JNDI_PROPERTY_PROVIDER_URL = "connectionFactoryJndiPropertyProviderUrl";
    private static final String PUB_SUB_DOMAIN = "pubSubDomain";

    private JmsFlowConnections() {}

    /**
     * A matched pair: producer sends to the same JMS destination consumer listens on, in two different Flows
     * of the same Module.
     */
    public record JmsLink(FlowElement producer, FlowElement consumer) {}

    /**
     * @param module to scan - the canvas only ever paints one Module at a time, so cross-Module matching isn't
     *               a meaningful concept here.
     * @return every matching producer/consumer pair found, in no particular order.
     */
    public static List<JmsLink> findMatchingLinks(Module module) {
        List<JmsLink> links = new ArrayList<>();
        if (module == null || module.getFlows() == null) {
            return links;
        }

        List<FlowElement> producers = new ArrayList<>();
        List<FlowElement> consumers = new ArrayList<>();
        for (Flow flow : module.getFlows()) {
            if (flow == null) {
                continue;
            }
            if (isJmsConsumer(flow.getConsumer())) {
                consumers.add(flow.getConsumer());
            }
            for (FlowElement element : flow.ftlGetConsumerAndFlowElements()) {
                if (isJmsProducer(element)) {
                    producers.add(element);
                }
            }
        }

        for (FlowElement producer : producers) {
            for (FlowElement consumer : consumers) {
                if (producer.getContainingFlow() != consumer.getContainingFlow() && sameDestination(producer, consumer)) {
                    links.add(new JmsLink(producer, consumer));
                }
            }
        }
        return links;
    }

    private static boolean isJms(FlowElement element) {
        return element != null && element.getComponentMeta() != null
                && JMS_ENDPOINT_KEY.equals(element.getComponentMeta().getEndpointKey());
    }

    private static boolean isJmsProducer(FlowElement element) {
        return isJms(element) && element.getComponentMeta().isProducer();
    }

    private static boolean isJmsConsumer(FlowElement element) {
        return isJms(element) && element.getComponentMeta().isConsumer();
    }

    private static boolean sameDestination(FlowElement producer, FlowElement consumer) {
        String destination = stringOrNull(producer.getPropertyValue(DESTINATION_JNDI_NAME));
        // A blank destination on either side is an incomplete/half-configured component, not a real match -
        // without this guard, every incomplete producer would match every incomplete consumer.
        if (destination == null || destination.isBlank()) {
            return false;
        }
        return destination.equals(stringOrNull(consumer.getPropertyValue(DESTINATION_JNDI_NAME)))
                && Objects.equals(stringOrNull(producer.getPropertyValue(CONNECTION_FACTORY_NAME)), stringOrNull(consumer.getPropertyValue(CONNECTION_FACTORY_NAME)))
                && Objects.equals(stringOrNull(producer.getPropertyValue(CONNECTION_FACTORY_JNDI_PROPERTY_PROVIDER_URL)), stringOrNull(consumer.getPropertyValue(CONNECTION_FACTORY_JNDI_PROPERTY_PROVIDER_URL)))
                && booleanOrDefault(producer.getPropertyValue(PUB_SUB_DOMAIN)) == booleanOrDefault(consumer.getPropertyValue(PUB_SUB_DOMAIN));
    }

    private static String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    private static boolean booleanOrDefault(Object value) {
        // pubSubDomain's own metadata default is false (queue) when unset - matches that convention here.
        return value instanceof Boolean bool && bool;
    }
}
