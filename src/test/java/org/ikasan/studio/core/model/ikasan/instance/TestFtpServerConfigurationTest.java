package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestFtpServerConfigurationTest {

    @BeforeAll
    static void warmUpMetaPack() throws Exception {
        ComponentLibrary.refreshComponentLibrary(TestFixtures.BASE_META_PACK);
    }
    @Test
    void resolvesConfiguredConsumerAddressAndCredentials() throws Exception {
        FlowElement consumer = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        consumer.setPropertyValue("remoteHost", "localhost");
        consumer.setPropertyValue("remotePort", "2121");
        consumer.setPropertyValue("username", "ikasan");
        consumer.setPropertyValue("password", "ikasan");

        TestFtpServerConfiguration configuration = TestFtpServerConfiguration.from(consumer);

        assertThat(configuration.host()).isEqualTo("localhost");
        assertThat(configuration.port()).isEqualTo(2121);
        assertThat(configuration.username()).isEqualTo("ikasan");
        assertThat(configuration.password()).isEqualTo("ikasan");
        assertThat(consumer.getComponentMeta().supportsTestFtpServer()).isTrue();
    }

    @Test
    void suppliesSafeNonPrivilegedDefaultsWhenValuesAreAbsent() throws Exception {
        FlowElement consumer = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        consumer.setPropertyValue("remoteHost", null);
        consumer.setPropertyValue("remotePort", null);
        consumer.setPropertyValue("username", null);
        consumer.setPropertyValue("password", null);

        assertThat(TestFtpServerConfiguration.from(consumer)).isEqualTo(
                new TestFtpServerConfiguration("127.0.0.1", 2121, "ikasan", "ikasan"));
    }
}
