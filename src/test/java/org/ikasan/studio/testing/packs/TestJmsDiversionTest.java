package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.analysis.JmsFlowConnections;
import org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestJmsDiversionTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void diversionSurvivesReloadAndRemovingOrUndoingItRestoresWiring(String pack) throws Exception {
        var producer = TestFixtures.getJmsProducer(pack);
        var consumer = TestFixtures.getSpringJmsConsumer(pack);
        for (var element : List.of(producer, consumer)) {
            element.setPropertyValue("destinationJndiName", "originalQueue");
            element.setPropertyValue("connectionFactoryName", "ConnectionFactory");
            element.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "vm://embedded-broker");
        }
        var upstream = flow(pack, "Upstream", TestFixtures.getEventGeneratingConsumer(pack), producer);
        var downstream = flow(pack, "Downstream", consumer, TestFixtures.getDevNullProducer(pack));
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>(List.of(upstream, downstream)));
        assertEquals(1, JmsFlowConnections.findMatchingLinks(module).size());
        String originalWiring = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, upstream);
        String downstreamWiring = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, downstream);

        String privateDestination = TestJmsHarnessLinks.newTestDestination(producer);
        assertNotEquals(privateDestination, TestJmsHarnessLinks.newTestDestination(producer));
        var testConsumer = TestFixtures.getSpringJmsConsumer(pack);
        testConsumer.setPropertyValue("destinationJndiName", privateDestination);
        var harness = flow(pack, "Test Reader", testConsumer, TestFixtures.getDevNullProducer(pack));
        harness.setPropertyValue("testHarnessOwner", TestJmsHarnessLinks.ownerKeyFor(producer));
        harness.setPropertyValue(TestJmsHarnessLinks.TEST_DESTINATION, privateDestination);
        module.addFlow(harness);

        assertEquals("originalQueue", producer.getPropertyValue("destinationJndiName"));
        assertEquals("originalQueue", consumer.getPropertyValue("destinationJndiName"));
        assertTrue(JmsFlowConnections.findMatchingLinks(module).isEmpty());
        String override = ".setDestinationJndiName(\"" + privateDestination + "\")";
        assertTrue(FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, upstream).contains(override));
        assertTrue(FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, harness).contains(override));
        assertEquals(downstreamWiring, FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, downstream));

        var reloaded = ComponentIO.validatePersistedModuleJson(ComponentIO.toJson(module), "test diversion", false);
        var link = TestJmsHarnessLinks.findLinks(reloaded).get(0);
        assertEquals(privateDestination, TestJmsHarnessLinks.destinationOverride(reloaded, link.ownerProducer()));
        assertEquals("originalQueue", link.ownerProducer().getPropertyValue("destinationJndiName"));
        reloaded.getFlows().remove(link.harnessFlow());
        assertNull(TestJmsHarnessLinks.destinationOverride(reloaded, link.ownerProducer()));
        assertEquals(1, JmsFlowConnections.findMatchingLinks(reloaded).size());

        module.getFlows().remove(harness);
        assertEquals(originalWiring, FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, upstream));
        module.addFlow(harness); // Undo removal reattaches the same persisted private destination.
        assertEquals(privateDestination, TestJmsHarnessLinks.destinationOverride(module, producer));
    }

    private static Flow flow(String pack, String name, FlowElement consumer, FlowElement output) throws Exception {
        Flow flow = new Flow(pack);
        flow.setName(name);
        flow.setConsumer(consumer);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        output.setContainingFlow(flow);
        output.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(output);
        return flow;
    }
}
