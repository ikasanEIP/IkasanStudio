package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;
import static org.ikasan.studio.core.TestFixtures.getJmsProducer;
import static org.ikasan.studio.core.TestFixtures.getSingleRecipientRouter;
import static org.ikasan.studio.core.TestFixtures.getSpringJmsConsumer;

class JmsFlowConnectionsTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    private static final String DESTINATION = "myQueue";
    private static final String CONNECTION_FACTORY = "myConnectionFactory";
    private static final String PROVIDER_URL = "vm://embedded-broker";

    @Test
    void matchesAProducerAndConsumerOnTheSameDestinationInDifferentFlows() throws StudioBuildException {
        Module module = moduleWith(
                flowWithProducer(destination(getJmsProducer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, false)),
                flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, false)));

        List<JmsFlowConnections.JmsLink> links = JmsFlowConnections.findMatchingLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).producer().getComponentMeta().isProducer()).isTrue();
        assertThat(links.get(0).consumer().getComponentMeta().isConsumer()).isTrue();
    }

    @Test
    void doesNotMatchDifferentDestinationNames() throws StudioBuildException {
        Module module = moduleWith(
                flowWithProducer(destination(getJmsProducer(BASE_META_PACK), "queueA", CONNECTION_FACTORY, PROVIDER_URL, false)),
                flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), "queueB", CONNECTION_FACTORY, PROVIDER_URL, false)));

        assertThat(JmsFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void doesNotMatchSameDestinationNameOnDifferentConnectionFactories() throws StudioBuildException {
        // Two unrelated brokers can coincidentally both have a queue literally named "myQueue" - the
        // connection factory / provider URL must also agree, or this isn't really the same destination.
        Module module = moduleWith(
                flowWithProducer(destination(getJmsProducer(BASE_META_PACK), DESTINATION, "factoryA", PROVIDER_URL, false)),
                flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), DESTINATION, "factoryB", PROVIDER_URL, false)));

        assertThat(JmsFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void doesNotMatchSameNameQueueAgainstTopic() throws StudioBuildException {
        Module module = moduleWith(
                flowWithProducer(destination(getJmsProducer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, false)),
                flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, true)));

        assertThat(JmsFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void doesNotMatchWhenBothSidesHaveABlankDestination() throws StudioBuildException {
        Module module = moduleWith(
                flowWithProducer(destination(getJmsProducer(BASE_META_PACK), "", CONNECTION_FACTORY, PROVIDER_URL, false)),
                flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), "", CONNECTION_FACTORY, PROVIDER_URL, false)));

        assertThat(JmsFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void findsAProducerBehindARouterBranch() throws StudioBuildException {
        FlowElement producer = destination(getJmsProducer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, false);
        FlowElement router = getSingleRecipientRouter(BASE_META_PACK);

        Flow producerFlow = new Flow(BASE_META_PACK);
        router.setContainingFlow(producerFlow);
        router.setContainingFlowRoute(producerFlow.getFlowRoute());
        producerFlow.getFlowRoute().getFlowElements().add(router);
        FlowRoute branch = FlowRoute.flowRouteBuilder().flow(producerFlow).flowElements(new java.util.ArrayList<>(List.of(producer))).build();
        producer.setContainingFlow(producerFlow);
        producer.setContainingFlowRoute(branch);
        producerFlow.getFlowRoute().getChildRoutes().add(branch);

        Module module = moduleWith(producerFlow, flowWithConsumer(destination(getSpringJmsConsumer(BASE_META_PACK), DESTINATION, CONNECTION_FACTORY, PROVIDER_URL, false)));

        List<JmsFlowConnections.JmsLink> links = JmsFlowConnections.findMatchingLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).producer()).isSameAs(producer);
    }

    private static FlowElement destination(FlowElement element, String destinationJndiName, String connectionFactoryName,
                                            String connectionFactoryJndiPropertyProviderUrl, boolean pubSubDomain) {
        element.setPropertyValue("destinationJndiName", destinationJndiName);
        element.setPropertyValue("connectionFactoryName", connectionFactoryName);
        element.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", connectionFactoryJndiPropertyProviderUrl);
        element.setPropertyValue("pubSubDomain", pubSubDomain);
        return element;
    }

    private static Flow flowWithProducer(FlowElement producer) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        producer.setContainingFlow(flow);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(producer);
        return flow;
    }

    private static Flow flowWithConsumer(FlowElement consumer) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        flow.setConsumer(consumer);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        return flow;
    }

    private static Module moduleWith(Flow... flows) throws StudioBuildException {
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flows));
    }
}
