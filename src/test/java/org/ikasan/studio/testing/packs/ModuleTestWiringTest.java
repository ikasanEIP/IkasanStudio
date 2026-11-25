package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.generator.FlowTestScaffold.Scaffold;
import org.ikasan.studio.core.migration.ModelMigration.Plan;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class ModuleTestWiringTest {
    @org.junit.jupiter.api.Test
    void migrationBaselineHasGeneratedWiring() throws Exception {
        Plan plan = org.ikasan.studio.core.migration.ModelMigration.analyse(
                Files.readString(Path.of("regression-tests/migration/baseline.json")), "V4.1.6");
        Module module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(plan.targetJson(), "wiring", false);
        assertNotNull(ModuleTestWiring.create(module));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void renamesAndConnectionsChangeOnlyWiringNotSharedJava(String version) throws Exception {
        Flow flow = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        flow.setConsumer(TestFixtures.getFtpConsumer(version));
        flow.getConsumer().setPropertyValue("password", "do-not-copy-this-secret");
        Module module = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        Scaffold before = FlowTestScaffold.render(module, flow, parent, app);
        String first = before.files().get(ModuleTestWiring.PATH);
        assertFalse(first.contains("do-not-copy-this-secret"));
        String oldName = flow.getIdentity();
        flow.setName("Renamed flow");
        Scaffold after = FlowTestScaffold.render(module, flow, parent, app);
        assertEquals(before.files().get(FlowTestScaffold.SUPPORT_PATH), after.files().get(FlowTestScaffold.SUPPORT_PATH));
        assertFalse(FlowTestScaffold.supportNeedsRefresh(before.files().get(FlowTestScaffold.SUPPORT_PATH), module));
        String second = after.files().get(ModuleTestWiring.PATH);
        assertNotEquals(first, second);
        JsonNode json = StudioJson.newObjectMapper().readTree(second);
        assertEquals("Renamed flow", json.path("ftpEndpoints").get(0).path("flow").asText());
        assertFalse(json.path("flowNames").toString().contains(oldName));
        assertEquals(FlowTestScaffold.supportFingerprint(module), json.path("modelFingerprint").asText());
        assertFalse(after.files().get(FlowTestScaffold.SUPPORT_PATH).contains("Renamed flow"));
        assertTrue(after.files().keySet().stream().noneMatch(path -> path.startsWith("generated/src/test/")));
    }
}
