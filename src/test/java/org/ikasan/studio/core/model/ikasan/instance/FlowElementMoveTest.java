package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;

class FlowElementMoveTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    @Test
    void reordersSameInstanceAndSupportsUndoRedo() throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        FlowElement first = attachedBroker(flow, "first");
        FlowElement moving = attachedBroker(flow, "moving");
        FlowElement last = attachedBroker(flow, "last");

        FlowElementMove.MoveResult result = FlowElementMove.move(moving, flow, flow.getFlowRoute(), 0);

        assertThat(result.accepted()).isTrue();
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(moving, first, last);
        result.undo(moving);
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(first, moving, last);
        result.redo(moving);
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(moving, first, last);
    }

    @Test
    void crossFlowMovePreservesIdentityPropertiesAndContainment() throws StudioBuildException {
        Flow source = new Flow(BASE_META_PACK);
        Flow target = new Flow(BASE_META_PACK);
        FlowElement moving = attachedBroker(source, "moving");
        moving.setPropertyValue("userImplementedClassName", "org.example.SpecialBroker");

        FlowElementMove.MoveResult result = FlowElementMove.move(moving, target, target.getFlowRoute(), 0);

        assertThat(result.accepted()).isTrue();
        assertThat(source.getFlowRoute().getFlowElements()).isEmpty();
        assertThat(target.getFlowRoute().getFlowElements()).containsExactly(moving);
        assertThat(target.getFlowRoute().getFlowElements().get(0)).isSameAs(moving);
        assertThat(moving.getPropertyValue("userImplementedClassName")).isEqualTo("org.example.SpecialBroker");
        assertThat(moving.getContainingFlow()).isSameAs(target);
        assertThat(moving.getContainingFlowRoute()).isSameAs(target.getFlowRoute());
    }

    @Test
    void invalidProducerMoveRestoresExactSourceAndTargetOrder() throws StudioBuildException {
        Flow source = new Flow(BASE_META_PACK);
        Flow target = new Flow(BASE_META_PACK);
        FlowElement moving = producer(source, "moving");
        FlowElement existing = producer(target, "existing");

        FlowElementMove.MoveResult result = FlowElementMove.move(moving, target, target.getFlowRoute(), 1);

        assertThat(result.accepted()).isFalse();
        assertThat(source.getFlowRoute().getFlowElements()).containsExactly(moving);
        assertThat(target.getFlowRoute().getFlowElements()).containsExactly(existing);
        assertThat(moving.getContainingFlow()).isSameAs(source);
    }

    @Test
    void noOpDropDoesNotReportAChange() throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        FlowElement only = attachedBroker(flow, "only");
        FlowElementMove.MoveResult result = FlowElementMove.move(only, flow, flow.getFlowRoute(), 0);
        assertThat(result.accepted()).isTrue();
        assertThat(result.changed()).isFalse();
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(only);
    }

    @Test
    void rejectsMovingAnOrdinaryComponentAfterAnExistingRouter() throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        FlowElement ordinary = attachedBroker(flow, "ordinary");
        FlowElement router = org.ikasan.studio.core.TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
        router.setContainingFlow(flow);
        router.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(router);

        FlowElementMove.MoveResult result = FlowElementMove.move(
                ordinary, flow, flow.getFlowRoute(), flow.getFlowRoute().getFlowElements().size());

        assertThat(result.accepted()).isFalse();
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(ordinary, router);
    }

    @Test
    void movesDebugToFirstRoutePositionImmediatelyAfterConsumer() throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumer(BASE_META_PACK));
        FlowElement filter = attachedBroker(flow, "filter");
        FlowElement debug = org.ikasan.studio.core.TestFixtures.getDebugTransition(BASE_META_PACK);
        debug.setContainingFlow(flow);
        debug.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(debug);
        FlowElement router = org.ikasan.studio.core.TestFixtures.getSingleRecipientRouter(BASE_META_PACK);
        router.setContainingFlow(flow);
        router.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(router);

        FlowElementMove.MoveResult result = FlowElementMove.move(debug, flow, flow.getFlowRoute(), 0);

        assertThat(result.accepted()).isTrue();
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(debug, filter, router);
    }

    private static FlowElement attachedBroker(Flow flow, String name) throws StudioBuildException {
        FlowElement element = getBroker(BASE_META_PACK);
        element.setComponentName(name);
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(element);
        return element;
    }

    private static FlowElement producer(Flow flow, String name) throws StudioBuildException {
        FlowElement element = org.ikasan.studio.core.TestFixtures.getGenericProducer(BASE_META_PACK);
        element.setComponentName(name);
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(element);
        return element;
    }
}
