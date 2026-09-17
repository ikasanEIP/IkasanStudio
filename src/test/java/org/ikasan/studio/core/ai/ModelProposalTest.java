package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

public class ModelProposalTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    public static final String CREATE = """
            [{"type":"addFlow","flow":"Transfer"},
             {"type":"addComponent","flow":"Transfer","key":"FTP Consumer","name":"ReadFiles",
              "properties":{"cronExpression":"0/5 * * * * ?","sourceDirectory":"/incoming"}},
             {"type":"addComponent","flow":"Transfer","key":"FTP Producer","name":"WriteFiles",
              "properties":{"outputDirectory":"/outgoing"}},
             {"type":"connect","flow":"Transfer","order":["ReadFiles","WriteFiles"]}]
            """;

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void validatesDetachedThenAppliesAndUndoesCompleteFlow(String version) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(version, new java.util.ArrayList<>());
        var before = LiveModelSnapshot.capture(live);
        var prepared = ModelProposal.prepare(before, JSON.readTree(CREATE));
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows()).hasSize(1);
        assertThat(live.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/incoming");
        String persisted = ComponentIO.toValidatedModuleJson(live);
        assertThat(persisted).contains("ReadFiles", "WriteFiles", "transitions");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(persisted);
    }

    @Test void editsRetainOriginalObjectsAndUndoRestoresValues() throws Exception {
        Module live = model();
        var flow = live.getFlows().get(0);
        var consumer = flow.getConsumer();
        var producer = flow.getFlowRoute().getFlowElements().get(0);
        var before = LiveModelSnapshot.capture(live);
        var prepared = ModelProposal.prepare(before, JSON.readTree("""
                [{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"sourceDirectory","value":"/new"}]
                """));
        var changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0)).isSameAs(flow);
        assertThat(flow.getConsumer()).isSameAs(consumer);
        assertThat(flow.getFlowRoute().getFlowElements().get(0)).isSameAs(producer);
        assertThat(consumer.getPropertyValue("sourceDirectory")).isEqualTo("/new");
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
    }

    @Test void rejectsInvalidProposalsWithoutChangingLiveModel() throws Exception {
        Module live = model();
        var snapshot = LiveModelSnapshot.capture(live);
        for (String operation : List.of(
                "{\"type\":\"deleteFlow\",\"flow\":\"Transfer\"}",
                "{\"type\":\"addFlow\",\"flow\":\"Transfer\"}",
                "{\"type\":\"setProperty\",\"flow\":\"Transfer\",\"component\":\"ReadFiles\",\"property\":\"notReal\",\"value\":true}",
                "{\"type\":\"setProperty\",\"flow\":\"Transfer\",\"component\":\"ReadFiles\",\"property\":\"ftps\",\"value\":\"yes\"}",
                "{\"type\":\"connect\",\"flow\":\"Transfer\",\"order\":[\"WriteFiles\",\"ReadFiles\"]}",
                "{\"type\":\"addComponent\",\"flow\":\"Transfer\",\"key\":\"Imaginary\",\"name\":\"Missing\"}")) {
            assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("[" + operation + "]"))).isInstanceOf(Exception.class);
            assertThat(LiveModelSnapshot.capture(live)).isEqualTo(snapshot);
        }
    }

    @Test void rejectsIncompleteFlowAndMissingRequiredProperties() throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of());
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree("[{\"type\":\"addFlow\",\"flow\":\"Empty\"}]")))
                .hasMessageContaining("consumer");
        Module live = model();
        assertThatThrownBy(() -> ModelProposal.prepare(LiveModelSnapshot.capture(live), JSON.readTree("""
                [{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"cronExpression","value":null}]
                """))).hasMessageContaining("missing");
    }

    @Test void snapshotIsDetachedAndPreservesUnknownData() throws Exception {
        Module live = model();
        live.getUnknownJsonProperties().put("future", JSON.readTree("{\"nested\":[1,2]}"));
        var captured = LiveModelSnapshot.capture(live);
        var restored = ComponentIO.validatePersistedModuleJson(JSON.writeValueAsString(captured), "test", false);
        assertThat(ComponentIO.toJson(restored)).isEqualTo(ComponentIO.toJson(live));
        live.getFlows().get(0).getConsumer().setPropertyValue("remoteHost", "different");
        assertThat(LiveModelSnapshot.capture(live)).isNotEqualTo(captured);
    }

    @Test void snapshotRoundTripsRoutersDecoratorsAndResolvers() throws Exception {
        String version = TestFixtures.BASE_META_PACK;
        for (var flow : List.of(TestFixtures.getEventGeneratingConsumerRouterFlow(version),
                TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(version),
                TestFixtures.getExceptionResolverFlow(version))) {
            // The legacy fixture uses "ignore"; a loaded Studio model uses "ignoreException".
            if (flow.getExceptionResolver() != null) {
                flow.getExceptionResolver().getIkasanExceptionResolutionMap().values().forEach(resolution -> {
                    if ("ignore".equals(resolution.getTheAction())) resolution.setTheAction("ignoreException");
                });
            }
            Module live = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
            var restored = ComponentIO.validatePersistedModuleJson(
                    JSON.writeValueAsString(LiveModelSnapshot.capture(live)), "snapshot round trip", false);
            assertThat(JSON.readTree(ComponentIO.toJson(restored))).isEqualTo(JSON.readTree(ComponentIO.toJson(live)));
        }
    }

    @Test void rejectsKnownPayloadMismatch() throws Exception {
        var snapshot = LiveModelSnapshot.capture(model());
        assertThatThrownBy(() -> ModelProposal.prepare(snapshot, JSON.readTree("""
                [{"type":"addComponent","flow":"Transfer","key":"Converter","name":"WrongType",
                  "properties":{"fromType":"java.lang.String","toType":"java.lang.Integer","userImplementedClassName":"WrongType"}}]
                """))).hasMessageContaining("mismatch");
    }

    public static Module model() throws Exception {
        Module empty = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of());
        return ModelProposal.prepare(LiveModelSnapshot.capture(empty), JSON.readTree(CREATE)).draft();
    }
}
