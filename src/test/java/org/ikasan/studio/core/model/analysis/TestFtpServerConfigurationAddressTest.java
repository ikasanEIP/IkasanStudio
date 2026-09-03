package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.getBroker;

/**
 * Covers {@link TestFtpServerConfiguration#sameAddressAs}, which decides whether a component is addressing the
 * running test FTP server, plus the metapack flag that decides which components may use it at all.
 */
class TestFtpServerConfigurationAddressTest {
    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        getBroker(BASE_META_PACK);
    }

    private static Stream<Arguments> equivalentLoopbackHosts() {
        return Stream.of(
                Arguments.of("127.0.0.1", "localhost"),
                Arguments.of("localhost", "127.0.0.1"),
                Arguments.of("127.0.0.1", "127.0.0.1"),
                Arguments.of("LOCALHOST", "localhost"),
                Arguments.of(" localhost ", "127.0.0.1"),
                Arguments.of("127.0.0.1", "127.0.1.1"),   // the whole 127.0.0.0/8 block is loopback
                Arguments.of("::1", "localhost"),
                Arguments.of("[::1]", "127.0.0.1"),
                Arguments.of("0:0:0:0:0:0:0:1", "::1")
        );
    }

    @ParameterizedTest
    @MethodSource("equivalentLoopbackHosts")
    void treatsLoopbackSpellingsOnTheSamePortAsOneAddress(String left, String right) {
        assertThat(configuration(left, 2121).sameAddressAs(configuration(right, 2121))).isTrue();
    }

    @Test
    void doesNotMatchADifferentPortOnTheSameHost() {
        assertThat(configuration("127.0.0.1", 2121).sameAddressAs(configuration("127.0.0.1", 2122))).isFalse();
    }

    @Test
    void doesNotMatchAGenuinelyRemoteHost() {
        assertThat(configuration("127.0.0.1", 2121).sameAddressAs(configuration("ftp.example.com", 2121))).isFalse();
        assertThat(configuration("10.0.0.5", 2121).sameAddressAs(configuration("127.0.0.1", 2121))).isFalse();
    }

    @Test
    void matchesIdenticalRemoteHostsSoNonLocalPairsStillGroup() {
        assertThat(configuration("ftp.example.com", 21).sameAddressAs(configuration("ftp.example.com", 21))).isTrue();
    }

    /**
     * Credentials are deliberately outside the address comparison - a credential mismatch is a runtime auth
     * failure, not a reason for the canvas to pretend the component points at a different server.
     */
    @Test
    void ignoresCredentialsWhenComparingAddresses() {
        TestFtpServerConfiguration left = new TestFtpServerConfiguration("127.0.0.1", 2121, "ikasan", "ikasan");
        TestFtpServerConfiguration right = new TestFtpServerConfiguration("localhost", 2121, "someoneElse", "other");
        assertThat(left.sameAddressAs(right)).isTrue();
    }

    @Test
    void toleratesANullComparand() {
        assertThat(configuration("127.0.0.1", 2121).sameAddressAs(null)).isFalse();
    }

    /**
     * The FTP Producer being absent from this set is precisely why a producer never linked to the running test
     * server on the canvas. Both sides are driven by the metapack flag, not by a hard-coded class/key check.
     */
    @Test
    void bothFtpConsumerAndFtpProducerDeclareTestFtpServerSupport() throws StudioBuildException {
        assertThat(TestFixtures.getFtpConsumer(BASE_META_PACK).getComponentMeta().supportsTestFtpServer()).isTrue();
        assertThat(TestFixtures.getFtpProducer(BASE_META_PACK).getComponentMeta().supportsTestFtpServer()).isTrue();
    }

    @Test
    void componentsWithNoFtpConnectionDoNotDeclareSupport() throws StudioBuildException {
        assertThat(TestFixtures.getDevNullProducer(BASE_META_PACK).getComponentMeta().supportsTestFtpServer()).isFalse();
        assertThat(getBroker(BASE_META_PACK).getComponentMeta().supportsTestFtpServer()).isFalse();
    }

    /** The producer's own remoteHost/remotePort resolve exactly as a consumer's do - one resolver, both sides. */
    @Test
    void resolvesProducerConnectionSettings() throws StudioBuildException {
        FlowElement producer = TestFixtures.getFtpProducer(BASE_META_PACK);
        producer.setPropertyValue("remoteHost", "localhost");
        producer.setPropertyValue("remotePort", 2121);

        TestFtpServerConfiguration configuration = TestFtpServerConfiguration.from(producer);

        assertThat(configuration.host()).isEqualTo("localhost");
        assertThat(configuration.port()).isEqualTo(2121);
        assertThat(configuration.address()).isEqualTo("localhost:2121");
    }

    private static TestFtpServerConfiguration configuration(String host, int port) {
        return new TestFtpServerConfiguration(host, port, "ikasan", "ikasan");
    }
}
