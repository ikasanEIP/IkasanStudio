package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTestWiringTest {
    @org.junit.jupiter.api.Test
    void migrationBaselineHasGeneratedWiring() throws Exception {
        var plan = org.ikasan.studio.core.migration.ModelMigration.analyse(
                Files.readString(Path.of("regression-tests/migration/baseline.json")), "V4.1.6");
        var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(plan.targetJson(), "wiring", false);
        assertNotNull(ModuleTestWiring.create(module));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void renamesAndConnectionsChangeOnlyWiringNotSharedJava(String version) throws Exception {
        var flow = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        flow.setConsumer(TestFixtures.getFtpConsumer(version));
        flow.getConsumer().setPropertyValue("password", "do-not-copy-this-secret");
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var before = FlowTestScaffold.render(module, flow, parent, app);
        String first = before.files().get(ModuleTestWiring.PATH);
        assertFalse(first.contains("do-not-copy-this-secret"));
        String oldName = flow.getIdentity();
        flow.setName("Renamed flow");
        var after = FlowTestScaffold.render(module, flow, parent, app);
        assertEquals(before.files().get(FlowTestScaffold.SUPPORT_PATH), after.files().get(FlowTestScaffold.SUPPORT_PATH));
        assertFalse(FlowTestScaffold.supportNeedsRefresh(before.files().get(FlowTestScaffold.SUPPORT_PATH), module));
        String second = after.files().get(ModuleTestWiring.PATH);
        assertNotEquals(first, second);
        var json = StudioJson.newObjectMapper().readTree(second);
        assertEquals("Renamed flow", json.path("ftpEndpoints").get(0).path("flow").asText());
        assertFalse(json.path("flowNames").toString().contains(oldName));
        assertEquals(FlowTestScaffold.supportFingerprint(module), json.path("modelFingerprint").asText());
        assertFalse(after.files().get(FlowTestScaffold.SUPPORT_PATH).contains("Renamed flow"));
        assertTrue(after.files().keySet().stream().noneMatch(path -> path.startsWith("generated/src/test/")));
    }
}
