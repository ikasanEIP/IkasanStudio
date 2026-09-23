package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestScaffoldTest {
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void scaffoldInheritsVersionAndMigrationPreservesModule(String version) throws Exception {
        String source = Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json"));
        var plan = ModelMigration.analyse(source, version);
        var module = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "test", false);
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var scaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        var pom = new MavenXpp3Reader().read(new StringReader(scaffold.files().get("user-flow-tests/pom.xml")));
        assertEquals("${version.ikasan}", pom.getDependencies().stream().filter(d -> d.getArtifactId().equals("ikasan-test")).findFirst().orElseThrow().getVersion());
        assertEquals(1, new MavenXpp3Reader().read(new StringReader(scaffold.rootPom())).getModules().stream().filter("user-flow-tests"::equals).count());
        assertEquals(scaffold.rootPom(), FlowTestScaffold.render(module, module.getFlows().get(0), scaffold.rootPom(), app).rootPom());
        var migrated = MigrationArtifacts.render(ModelMigration.analyse(plan.targetJson(), version.equals("V3.3.9") ? "V4.1.6" : "V3.3.9"), scaffold.rootPom());
        assertTrue(new MavenXpp3Reader().read(new StringReader(migrated.get("pom.xml"))).getModules().contains("user-flow-tests"));
        assertTrue(migrated.keySet().stream().noneMatch(p -> p.startsWith("user-flow-tests/")));
        String test = scaffold.files().get(scaffold.testPath());
        assertTrue(test.indexOf("assertTrue(\"Complete") < test.indexOf("try (ConfigurableApplicationContext"));
        assertTrue(test.contains("flowStartupTypes[0]=MyFlow1,MANUAL"));
        var local = org.ikasan.studio.core.TestFixtures.getLocalFileConsumer(version);
        module.getFlows().get(0).setConsumer(local);
        var localScaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        String localTest = localScaffold.files().get(localScaffold.testPath());
        assertTrue(localTest.contains("new org.junit.rules.TemporaryFolder()"));
        assertTrue(localTest.contains("java.nio.file.Files.writeString"));
        assertTrue(localTest.contains("harness.assertIsSatisfied()"));
        assertTrue(localTest.contains(".repeat(2)"));
        assertTrue(localTest.contains(".scheduledConsumer(\"" + local.getIdentity() + "\")"));
        assertTrue(localTest.contains("--myflow1.file.consumer.filenames="));
        module.getFlows().get(0).getFlowRoute().getChildRoutes().add(
                org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                        .flow(module.getFlows().get(0)).routeName("Branch").build());
        var branched = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        String branchedTest = branched.files().get(branched.testPath());
        assertFalse(branchedTest.contains(".repeat(2)"));
        assertTrue(branchedTest.contains("Define the expected component path for both input batches"));
        assertTrue(localTest.contains("int batch) throws Exception"));
        assertTrue(localTest.contains("harness.fireScheduledConsumer()"));
        assertTrue(localTest.contains("FIRST_EXPECTED, outputs.poll"));
        assertTrue(localTest.contains("SECOND_EXPECTED, outputs.poll"));
        assertTrue(localTest.contains("CONFIGURED = false"));
        for (int task = 1; task <= 5; task++) assertTrue(localTest.contains("// TODO " + task + ":"));
    }
}
