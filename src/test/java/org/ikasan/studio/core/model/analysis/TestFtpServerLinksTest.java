package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;

class TestFtpServerLinksTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    /**
     * The canvas draws the test FTP server node on the side the files actually travel from/to, so the grouping
     * has to keep consumers and producers apart even when they share one address - see TestFtpServerLinks.
     */
    @Test
    void separatesConsumersFromProducersSharingOneAddress() throws StudioBuildException {
        FlowElement consumer = ftpConsumerAt("127.0.0.1", 2121);
        FlowElement producer = ftpProducerAt("127.0.0.1", 2121);
        Module module = moduleWith(flowWithConsumer(consumer), flowWithElement(producer));

        List<TestFtpServerLinks.Link> links = TestFtpServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).consumers()).containsExactly(consumer);
        assertThat(links.get(0).producers()).containsExactly(producer);
        assertThat(links.get(0).address()).isEqualTo("127.0.0.1:2121");
    }

    /**
     * The exact reason an FTP Producer showed no link to an already-running test server: a Consumer written as
     * "127.0.0.1" and a Producer written as "localhost" address the same endpoint and must share one Link.
     */
    @Test
    void groupsLoopbackSpellingsOfTheSameHostTogether() throws StudioBuildException {
        FlowElement consumer = ftpConsumerAt("127.0.0.1", 2121);
        FlowElement producer = ftpProducerAt("localhost", 2121);
        Module module = moduleWith(flowWithConsumer(consumer), flowWithElement(producer));

        List<TestFtpServerLinks.Link> links = TestFtpServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).consumers()).containsExactly(consumer);
        assertThat(links.get(0).producers()).containsExactly(producer);
    }

    @Test
    void keepsGenuinelyDifferentAddressesApart() throws StudioBuildException {
        FlowElement consumer = ftpConsumerAt("127.0.0.1", 2121);
        FlowElement producer = ftpProducerAt("127.0.0.1", 2122);
        Module module = moduleWith(flowWithConsumer(consumer), flowWithElement(producer));

        assertThat(TestFtpServerLinks.findLinks(module)).hasSize(2);
    }

    /** More than one producer may share the single right-hand node. */
    @Test
    void groupsSeveralProducersOntoOneLink() throws StudioBuildException {
        FlowElement firstProducer = ftpProducerAt("localhost", 2121);
        FlowElement secondProducer = ftpProducerAt("127.0.0.1", 2121);
        Module module = moduleWith(flowWithElement(firstProducer), flowWithElement(secondProducer));

        List<TestFtpServerLinks.Link> links = TestFtpServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).consumers()).isEmpty();
        assertThat(links.get(0).producers()).containsExactly(firstProducer, secondProducer);
    }

    @Test
    void ignoresComponentsThatCannotUseTheTestFtpServer() throws StudioBuildException {
        Module module = moduleWith(flowWithElement(TestFixtures.getDevNullProducer(BASE_META_PACK)));

        assertThat(TestFtpServerLinks.findLinks(module)).isEmpty();
    }

    @Test
    void toleratesANullModule() {
        assertThat(TestFtpServerLinks.findLinks(null)).isEmpty();
    }

    private static FlowElement ftpConsumerAt(String host, int port) throws StudioBuildException {
        FlowElement consumer = TestFixtures.getFtpConsumer(BASE_META_PACK);
        consumer.setPropertyValue("remoteHost", host);
        consumer.setPropertyValue("remotePort", port);
        return consumer;
    }

    private static FlowElement ftpProducerAt(String host, int port) throws StudioBuildException {
        FlowElement producer = TestFixtures.getFtpProducer(BASE_META_PACK);
        producer.setPropertyValue("remoteHost", host);
        producer.setPropertyValue("remotePort", port);
        return producer;
    }

    private static Flow flowWithConsumer(FlowElement consumer) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        flow.setConsumer(consumer);
        return flow;
    }

    private static Flow flowWithElement(FlowElement element) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(element);
        return flow;
    }

    private static Module moduleWith(Flow... flows) throws StudioBuildException {
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flows));
    }
}
