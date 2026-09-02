package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.model.analysis.TestMailServerLinks;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;

class TestMailServerLinksTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    @Test
    void groupsProducersSharingTheSameAddressIntoOneLink() throws StudioBuildException {
        FlowElement producerA = emailProducer("producerA", "127.0.0.1", 1025);
        FlowElement producerB = emailProducer("producerB", "127.0.0.1", 1025);
        Module module = moduleWith(flowWithProducer(producerA), flowWithProducer(producerB));

        List<TestMailServerLinks.Link> links = TestMailServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).host()).isEqualTo("127.0.0.1");
        assertThat(links.get(0).port()).isEqualTo(1025);
        assertThat(links.get(0).producers()).containsExactlyInAnyOrder(producerA, producerB);
    }

    @Test
    void splitsProducersConfiguredForDifferentAddressesIntoSeparateLinks() throws StudioBuildException {
        FlowElement producerA = emailProducer("producerA", "127.0.0.1", 1025);
        FlowElement producerB = emailProducer("producerB", "127.0.0.1", 2525);
        Module module = moduleWith(flowWithProducer(producerA), flowWithProducer(producerB));

        assertThat(TestMailServerLinks.findLinks(module)).hasSize(2);
    }

    @Test
    void fallsBackToTheDefaultAddressWhenUnconfigured() throws StudioBuildException {
        FlowElement producer = emailProducerWithNoSmtpConfig("producerA");
        Module module = moduleWith(flowWithProducer(producer));

        List<TestMailServerLinks.Link> links = TestMailServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).host()).isEqualTo(TestMailServerLinks.DEFAULT_SMTP_HOST);
        assertThat(links.get(0).port()).isEqualTo(TestMailServerLinks.DEFAULT_SMTP_PORT);
    }

    @Test
    void fallsBackToMailhostWhenMailSmtpHostIsUnsetButMailhostIsSet() throws StudioBuildException {
        FlowElement producer = emailProducerWithNoSmtpConfig("producerA");
        producer.setPropertyValue("mailhost", "smtp.example.com");
        Module module = moduleWith(flowWithProducer(producer));

        List<TestMailServerLinks.Link> links = TestMailServerLinks.findLinks(module);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).host()).isEqualTo("smtp.example.com");
    }

    @Test
    void ignoresNonEmailProducerComponents() throws StudioBuildException {
        Module module = moduleWith(flowWithProducer(TestFixtures.getDevNullProducer(BASE_META_PACK)));

        assertThat(TestMailServerLinks.findLinks(module)).isEmpty();
    }

    private static FlowElement emailProducer(String componentName, String smtpHost, int smtpPort) throws StudioBuildException {
        FlowElement producer = emailProducerWithNoSmtpConfig(componentName);
        producer.setPropertyValue("mailSmtpHost", smtpHost);
        producer.setPropertyValue("mailSmtpPort", smtpPort);
        return producer;
    }

    private static FlowElement emailProducerWithNoSmtpConfig(String componentName) throws StudioBuildException {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName(componentName)
                .build();
    }

    private static Flow flowWithProducer(FlowElement producer) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        producer.setContainingFlow(flow);
        producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().getFlowElements().add(producer);
        return flow;
    }

    private static Module moduleWith(Flow... flows) throws StudioBuildException {
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flows));
    }
}
