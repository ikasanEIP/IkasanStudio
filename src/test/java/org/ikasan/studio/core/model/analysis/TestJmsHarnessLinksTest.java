package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;

class TestJmsHarnessLinksTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    @Test
    void pairsAHarnessFlowWithItsOwnerProducerAndDebugComponent() throws StudioBuildException {
        FlowElement producer = jmsProducer("myProducer");
        Flow ownerFlow = flowWithProducer("ownerFlow", producer);
        FlowElement debug = TestFixtures.getDebugTransition(BASE_META_PACK);
        Flow harnessFlow = harnessFlowFor("Test myQueue", producer, debug);
        Module module = moduleWith(ownerFlow, harnessFlow);

        List<TestJmsHarnessLinks.Link> links = TestJmsHarnessLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).ownerProducer()).isSameAs(producer);
        assertThat(links.get(0).harnessFlow()).isSameAs(harnessFlow);
        assertThat(links.get(0).debugComponent()).isSameAs(debug);
    }

    @Test
    void ignoresOrdinaryFlowsWithNoTestHarnessOwnerSet() throws StudioBuildException {
        FlowElement producer = jmsProducer("myProducer");
        Module module = moduleWith(flowWithProducer("ownerFlow", producer));

        assertThat(TestJmsHarnessLinks.findLinks(module)).isEmpty();
    }

    @Test
    void ignoresAHarnessFlowWhoseOwnerCanNoLongerBeResolved() throws StudioBuildException {
        Flow harnessFlow = new Flow(BASE_META_PACK);
        harnessFlow.setName("Test orphaned");
        harnessFlow.setPropertyValue("testHarnessOwner", "no-such-flow/no-such-component");
        Module module = moduleWith(harnessFlow);

        assertThat(TestJmsHarnessLinks.findLinks(module)).isEmpty();
    }

    @Test
    void ownerKeyJoinsFlowAndComponentIdentity() throws StudioBuildException {
        FlowElement producer = jmsProducer("myProducer");
        flowWithProducer("ownerFlow", producer);

        assertThat(TestJmsHarnessLinks.ownerKeyFor(producer)).isEqualTo("ownerFlow/myProducer");
    }

    private static FlowElement jmsProducer(String componentName) throws StudioBuildException {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Spring JMS Producer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName(componentName)
                .build();
    }

    private static Flow flowWithProducer(String flowName, FlowElement producer) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        flow.setName(flowName);
        producer.setContainingFlow(flow);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(producer);
        return flow;
    }

    private static Flow harnessFlowFor(String flowName, FlowElement owner, FlowElement debug) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        flow.setName(flowName);
        flow.setPropertyValue("testHarnessOwner", TestJmsHarnessLinks.ownerKeyFor(owner));
        FlowElement consumer = TestFixtures.getSpringJmsConsumer(BASE_META_PACK);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        flow.setConsumer(consumer);
        debug.setContainingFlow(flow);
        debug.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(debug);
        return flow;
    }

    private static Module moduleWith(Flow... flows) throws StudioBuildException {
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flows));
    }
}
