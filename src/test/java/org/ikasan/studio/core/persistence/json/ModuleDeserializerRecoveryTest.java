package org.ikasan.studio.core.persistence.json;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Transition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

class ModuleDeserializerRecoveryTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
    }

    @Test
    void preservesElementsFollowingAnInvalidlyMovedRouter() throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        FlowElement consumer = named(TestFixtures.getEventGeneratingConsumer(BASE_META_PACK), "egc", flow);
        FlowElement router = named(TestFixtures.getSingleRecipientRouter(BASE_META_PACK), "dd", flow);
        FlowElement filter = named(TestFixtures.getBroker(BASE_META_PACK), "filt", flow);
        FlowElement debug = named(TestFixtures.getDebugTransition(BASE_META_PACK), "ddDebug", flow);
        FlowElement route1Producer = named(TestFixtures.getLoggingProducer(BASE_META_PACK), "lp1", flow);
        FlowElement route2Producer = named(TestFixtures.getLoggingProducer(BASE_META_PACK), "lp2", flow);
        flow.setConsumer(consumer);

        Map<String, FlowElement> elements = new LinkedHashMap<>();
        for (FlowElement element : List.of(consumer, router, filter, debug, route1Producer, route2Producer)) {
            elements.put(element.getIdentity(), element);
        }
        List<Transition> transitions = List.of(
                transition("egc", "dd", "default"),
                transition("dd", "filt", "default"),
                transition("filt", "ddDebug", "default"),
                transition("ddDebug", "lp1", "route1"),
                transition("ddDebug", "lp2", "route2"));

        FlowRoute root = new ModuleDeserializer().orderFlowElementsByTransitions(
                BASE_META_PACK, transitions, flow, elements);

        assertThat(root.getFlowElements()).extracting(FlowElement::getIdentity)
                .containsExactly("filt", "ddDebug", "dd");
        assertThat(root.getChildRoutes()).extracting(FlowRoute::getRouteName)
                .containsExactly("route1", "route2");
        assertThat(root.getChildRoutes().get(0).getFlowElements()).extracting(FlowElement::getIdentity)
                .contains("lp1");
        assertThat(root.getChildRoutes().get(1).getFlowElements()).extracting(FlowElement::getIdentity)
                .contains("lp2");

        flow.setFlowRoute(root);
        String recoveredJson = ComponentIO.toJson(flow);
        assertThat(recoveredJson).contains("\"from\":\"dd\",\"to\":\"lp1\",\"name\":\"route1\"");
        assertThat(recoveredJson).contains("\"from\":\"dd\",\"to\":\"lp2\",\"name\":\"route2\"");
        assertThat(recoveredJson).doesNotContain("\"from\":\"dd\",\"to\":\"filt\"");
    }

    private static FlowElement named(FlowElement element, String name, Flow flow) {
        element.setComponentName(name);
        element.setContainingFlow(flow);
        return element;
    }

    private static Transition transition(String from, String to, String name) {
        return Transition.builder().from(from).to(to).name(name).build();
    }
}
