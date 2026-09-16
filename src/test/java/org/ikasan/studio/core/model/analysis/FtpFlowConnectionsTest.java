package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.*;

class FtpFlowConnectionsTest {
    private FlowElement producer;
    private FlowElement consumer;
    private Module module;

    @BeforeEach
    void setup() throws StudioBuildException {
        getBroker(BASE_META_PACK);
        producer = getFtpProducer(BASE_META_PACK);
        consumer = getFtpConsumer(BASE_META_PACK);
        producer.setPropertyValue("ftps", false);
        consumer.setPropertyValue("ftps", false);
        consumer.setPropertyValue("sourceDirectoryURLFactory", "");
        producer.setPropertyValue("outputDirectory", "/inbox/");
        consumer.setPropertyValue("sourceDirectory", "/inbox");
        producer.setPropertyValue("remoteHost", "FILES.example");
        consumer.setPropertyValue("remoteHost", "files.example");
        Flow sending = new Flow(BASE_META_PACK);
        producer.setContainingFlow(sending);
        producer.setContainingFlowRoute(sending.getFlowRoute());
        sending.getFlowRoute().getFlowElements().add(producer);
        Flow receiving = new Flow(BASE_META_PACK);
        receiving.setConsumer(consumer);
        consumer.setContainingFlow(receiving);
        consumer.setContainingFlowRoute(receiving.getFlowRoute());
        module = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(sending, receiving));
    }

    @Test
    void matchesAndShowsOnlySelectedEndpointsUnlessShowAllIsEnabled() {
        var links = FtpFlowConnections.findMatchingLinks(module);
        assertThat(links).hasSize(1);
        var link = links.get(0);
        assertThat(link.endpoint().address()).isEqualTo("ftp://files.example:1024 /inbox");
        assertThat(link.isVisible(producer, false)).isTrue();
        assertThat(link.isVisible(consumer, false)).isTrue();
        assertThat(link.isVisible(module, false)).isFalse();
        assertThat(link.isVisible(null, true)).isTrue();
    }

    @Test
    void rejectsDifferentPortsAndReflectsEdits() {
        consumer.setPropertyValue("remotePort", 21);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
        consumer.setPropertyValue("remotePort", 1024);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).hasSize(1);
        consumer.setPropertyValue("remoteHost", "other.example");
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void respectsPathCaseAndRelativePaths() {
        for (String path : List.of("/Inbox", "inbox", "/other", " ", "${directory}", "/inbox/../inbox")) {
            consumer.setPropertyValue("sourceDirectory", path);
            assertThat(FtpFlowConnections.findMatchingLinks(module)).as(path).isEmpty();
        }
    }

    @Test
    void skipsUnknownEndpointsAndCustomDirectoryFactories() {
        consumer.setPropertyValue("sourceDirectoryURLFactory", "CustomDirectoryFactory");
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
        consumer.setPropertyValue("sourceDirectoryURLFactory", "");
        for (String port : List.of("${port}", "oops", "0", "65536")) {
            consumer.setPropertyValue("remotePort", port);
            assertThat(FtpFlowConnections.findMatchingLinks(module)).as(port).isEmpty();
        }
    }

    @Test
    void usesFtpsPortAndRequiresSameSecurityMode() {
        consumer.setPropertyValue("ftps", true);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
        producer.setPropertyValue("ftps", true);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).hasSize(1);
        consumer.setPropertyValue("ftpsIsImplicit", false);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
        producer.setPropertyValue("ftpsIsImplicit", false);
        consumer.setPropertyValue("ftpsPort", 999);
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
    }

    @Test
    void moduleIsolationAndRemoval() throws StudioBuildException {
        assertThat(FtpFlowConnections.findMatchingLinks(null)).isEmpty();
        assertThat(FtpFlowConnections.findMatchingLinks(TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of()))).isEmpty();
        module.getFlows().get(0).getFlowRoute().getFlowElements().clear();
        assertThat(FtpFlowConnections.findMatchingLinks(module)).isEmpty();
    }
}
