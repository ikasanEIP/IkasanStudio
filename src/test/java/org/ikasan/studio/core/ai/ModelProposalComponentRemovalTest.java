package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.ikasan.studio.core.ai.ModelProposal.ChangeSet;
import org.ikasan.studio.core.ai.ModelProposal.Prepared;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

class ModelProposalComponentRemovalTest {
    private final ObjectMapper json = new ObjectMapper();

    private Module model(String version) throws Exception {
        return ModelProposal.prepare(LiveModelSnapshot.capture(TestFixtures.getMyFirstModuleIkasanModule(version, new ArrayList<>())),
                json.readTree("""
                [{"type":"addFlow","flow":"toby"},
                 {"type":"addComponent","flow":"toby","key":"Event Generating Consumer","name":"bob",
                  "properties":{"endpointEventProvider":"MinuteEventProvider"}},
                 {"type":"addComponent","flow":"toby","key":"Dev Null Producer","name":"timmy"}]
                """)).draft();
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void replacesConsumerWithJmsAndPreservesDownstreamIdentityAndUndo(String version) throws Exception {
        Module live = model(version);
        Map<String,Object> before = LiveModelSnapshot.capture(live);
        Flow flow = live.getFlows().get(0);
        FlowElement bob = flow.getConsumer();
        FlowElement timmy = flow.getFlowRoute().getFlowElements().get(0);
        Prepared prepared = ModelProposal.prepare(before, json.readTree("""
                [{"type":"replaceComponent","flow":"toby","component":"bob","key":"Spring JMS Consumer",
                  "name":"Receive from Tom","properties":{"destinationJndiName":"tom.to.toby"}}]
                """));
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        ChangeSet changes = ModelProposal.changes(live, prepared);
        changes.apply();
        FlowElement replacement = flow.getConsumer();
        assertThat(replacement).isNotSameAs(bob);
        assertThat(replacement.getIdentity()).isEqualTo("Receive from Tom");
        assertThat(replacement.getPropertyValue("destinationJndiName")).isEqualTo("tom.to.toby");
        assertThat(bob.getPropertyValue("endpointEventProvider")).isEqualTo("MinuteEventProvider");
        assertThat(flow.getFlowRoute().getFlowElements()).containsExactly(timmy);
        String persisted = ComponentIO.toValidatedModuleJson(live);
        assertThat(ComponentIO.validatePersistedModuleJson(persisted, "test", false).getFlows().get(0).getConsumer().getIdentity())
                .isEqualTo("Receive from Tom");
        assertThat(FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, live, flow)).contains("Receive from Tom", "timmy");
        assertThat(FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, live, flow)).contains("jmsConsumer");
        changes.undo();
        assertThat(flow.getConsumer()).isSameAs(bob);
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(flow.getConsumer()).isSameAs(replacement);
        assertThat(ComponentIO.toValidatedModuleJson(live)).isEqualTo(persisted);
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void deletesBothComponentsAndCanUndoOrReuseTheSameName(String version) throws Exception {
        Module live = model(version);
        Map<String,Object> before = LiveModelSnapshot.capture(live);
        FlowElement old = live.getFlows().get(0).getConsumer();
        Prepared prepared = ModelProposal.prepare(before, json.readTree("""
                [{"type":"deleteComponent","flow":"toby","component":"bob"},
                 {"type":"deleteComponent","flow":"toby","component":"timmy"}]
                """));
        ChangeSet changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0).getConsumer()).isNull();
        assertThat(live.getFlows().get(0).getFlowRoute().getFlowElements()).isEmpty();
        ComponentIO.toValidatedModuleJson(live);
        changes.undo();
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        Prepared replacement = ModelProposal.prepare(before, json.readTree("""
                [{"type":"deleteComponent","flow":"toby","component":"bob"},
                 {"type":"addComponent","flow":"toby","key":"Spring JMS Consumer","name":"bob",
                  "properties":{"destinationJndiName":"tom.to.toby"}},
                 {"type":"replaceComponent","flow":"toby","component":"timmy","key":"Dev Null Producer","name":"timmy"}]
                """));
        ModelProposal.changes(live, replacement).apply();
        assertThat(live.getFlows().get(0).getConsumer()).isNotSameAs(old);
        assertThat(live.getFlows().get(0).getConsumer().getPropertyValue("destinationJndiName")).isEqualTo("tom.to.toby");
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void rejectsInvalidReplacementWithoutTouchingLiveModel(String version) throws Exception {
        Module live = model(version);
        Map<String,Object> before = LiveModelSnapshot.capture(live);
        for (String operation : java.util.List.of(
                "{\"type\":\"deleteComponent\",\"flow\":\"toby\",\"component\":\"missing\"}",
                "{\"type\":\"replaceComponent\",\"flow\":\"toby\",\"component\":\"bob\",\"key\":\"Dev Null Producer\",\"name\":\"new\"}",
                "{\"type\":\"replaceComponent\",\"flow\":\"toby\",\"component\":\"bob\",\"key\":\"Spring JMS Consumer\",\"name\":\"timmy\"}",
                "{\"type\":\"replaceComponent\",\"flow\":\"toby\",\"component\":\"bob\",\"key\":\"Spring JMS Consumer\",\"name\":\"new\",\"properties\":{\"unknown\":true}}")) {
            assertThatThrownBy(() -> ModelProposal.prepare(before, json.readTree("[" + operation + "]"))).isInstanceOf(Exception.class);
            assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        }
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void deletesWholeFlowAndHarnessWithIdentityPreservingUndoAndRedo(String version) throws Exception {
        Module live = model(version);
        Flow original = live.getFlows().get(0);
        Flow harness = new org.ikasan.studio.core.model.ikasan.instance.Flow(version);
        harness.setName("TestHarness");
        harness.setPropertyValue("testHarnessOwner", "toby/timmy");
        live.getFlows().add(harness);
        Map<String,Object> before = LiveModelSnapshot.capture(live);
        Prepared prepared = ModelProposal.prepare(before, json.readTree("""
                [{"type":"deleteFlow","flow":"toby"}]
                """));
        assertThat(prepared.deletesContent()).isTrue();
        assertThat(prepared.draft().getFlows()).isEmpty();
        assertThat(prepared.summary()).anyMatch(line -> line.contains("TestHarness"));
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        ChangeSet changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows()).isEmpty();
        ComponentIO.toValidatedModuleJson(live);
        changes.undo();
        assertThat(live.getFlows()).containsExactly(original, harness);
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
        changes.apply();
        assertThat(live.getFlows()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void deleteAndRecreateSameFlowNameUsesNewFlowAndRestoresOriginalOnUndo(String version) throws Exception {
        Module live = model(version);
        Flow original = live.getFlows().get(0);
        Prepared prepared = ModelProposal.prepare(LiveModelSnapshot.capture(live), json.readTree("""
                [{"type":"deleteFlow","flow":"toby"},{"type":"addFlow","flow":"toby"}]
                """));
        ChangeSet changes = ModelProposal.changes(live, prepared);
        changes.apply();
        assertThat(live.getFlows().get(0)).isNotSameAs(original);
        assertThat(live.getFlows().get(0).getConsumer()).isNull();
        changes.undo();
        assertThat(live.getFlows()).containsExactly(original);
    }

}
