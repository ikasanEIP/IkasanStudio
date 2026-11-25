package org.ikasan.studio.packs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.migration.MigrationRules;
import org.ikasan.studio.core.migration.ModelMigration;
import org.ikasan.studio.core.migration.UserImportMigration;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import org.ikasan.studio.core.migration.ModelMigration.Plan;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

/** Explicit development rehearsal; the released migration paths remain unchanged. */
class MigrationFixtureTest {
    private static final String TARGET = DevelopmentFixtureTest.PACK;
    private static final List<String> SOURCES = List.of("V3.3.9", "V4.1.6");
    @TempDir Path temporary;

    private MigrationRules rules(String source) throws Exception {
        return ComponentIO.deserializeResource("studio/metapack/" + TARGET + "/migrations/from-" + source + ".json",
                MigrationRules.class);
    }

    private FlowElement element(String version, String key, String name) throws Exception {
        FlowElement result = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(version, key))
                .componentName(name).build();
        result.defaultUnsetMandatoryProperties();
        return result;
    }

    private String source(String version, boolean converter) throws Exception {
        Flow flow = Flow.flowBuilder().metapackVersion(version).name("Example Flow")
                .consumer(element(version, "Event Generating Consumer", "Input")).build();
        ArrayList<FlowElement> elements = new ArrayList<>();
        if (converter) {
            FlowElement component = element(version, "Converter", "Convert");
            component.setPropertyValue("userImplementedClassName", "ConvertMessage");
            component.setPropertyValue("fromType", version.startsWith("V3") ? "javax.jms.Message" : "jakarta.jms.Message");
            component.setPropertyValue("toType", "javax.mail.Message");
            elements.add(component);
        }
        elements.add(element(version, "Dev Null Producer", "Output"));
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(elements).build());
        Module module = Module.moduleBuilder().version(version).name("MigrationReview")
                .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(List.of(flow)).build();
        module.setPropertyValue("flowStartupType", "MANUAL");
        return ComponentIO.toJson(module);
    }

    @Test void rehearsesBothSourcesAndExportsRunnableTargets() throws Exception {
        Path root = Path.of(System.getProperty("studio.v5.migration", temporary.toString()));
        for (String version : SOURCES) {
            String original = source(version, false);
            Plan plan = ModelMigration.analyseWithRules(original, rules(version));
            assertTrue(plan.canApply(), plan.report());
            assertEquals(original, plan.sourceJson());
            assertTrue(plan.report().contains("Java build baseline to 25"));
            Module module = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "V5 migration rehearsal", false);
            Map<String,String> files = DevelopmentFixtureTest.render(module);
            files.compute("pom.xml", (key, value) -> Objects.requireNonNull(value, key).replace("<artifactId>ikasan5-review</artifactId>",
                    "<artifactId>migration-" + version.toLowerCase(Locale.ROOT) + "</artifactId>"));
            files.put("migration-source.json", original);
            files.put("migration-target.json", plan.targetJson());
            files.put("migration-report.txt", "DEVELOPMENT REHEARSAL ONLY: not an enabled IDE migration path.\n"
                    + "User classes and existing flow tests are not rewritten by this fixture.\n" + plan.report());
            for (Entry<String,String> file : files.entrySet()) {
                Path target = root.resolve(version).resolve(file.getKey());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue());
            }
        }
        Files.writeString(root.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion>
                <groupId>org.ikasan.studio.probe</groupId><artifactId>migration-rehearsal</artifactId>
                <version>1</version><packaging>pom</packaging>
                <modules><module>V3.3.9</module><module>V4.1.6</module></modules></project>
                """);
    }

    @Test void checksCompleteCatalogueMappingsAndPreservesOpaqueData() throws Exception {
        ObjectMapper json = StudioJson.newObjectMapper();
        for (String version : SOURCES) {
            MigrationRules rules = rules(version);
            assertEquals(ComponentLibrary.getIkasanComponents(version).keySet(), rules.componentMappings().keySet());
            for (Entry<String,String> mapping : rules.componentMappings().entrySet()) {
                assertEquals(ComponentLibrary.getIkasanComponentByKeyMandatory(version, mapping.getKey()).getComponentType(),
                        ComponentLibrary.getIkasanComponentByKeyMandatory(TARGET, mapping.getValue()).getComponentType());
            }
            ObjectNode original = (ObjectNode) json.readTree(source(version, true));
            original.putObject("teamSettings").put("literal", "javax.jms.Message");
            Plan plan = ModelMigration.analyseWithRules(original.toString(), rules);
            assertTrue(plan.canApply(), plan.report());
            JsonNode target = json.readTree(plan.targetJson());
            assertEquals(original.path("teamSettings"), target.path("teamSettings"));
            assertEquals(original.path("flows").get(0).path("transitions"), target.path("flows").get(0).path("transitions"));
            JsonNode component = target.path("flows").get(0).path("flowElements").get(0);
            assertEquals("jakarta.jms.Message", component.path("fromType").asText());
            assertEquals("javax.mail.Message", component.path("toType").asText());
            assertEquals("ConvertMessage", component.path("userImplementedClassName").asText());
            assertTrue(plan.report().contains("user-implemented class is preserved"));
            String imports = """
                    import javax.jms.Message;
                    import javax.mail.Session;
                    // javax.jms.Message is documentation
                    """;
            String rewritten = UserImportMigration.rewrite(imports, rules.userImportPrefixes());
            assertTrue(rewritten.contains("import javax.mail.Session;"));
            assertTrue(rewritten.contains("// javax.jms.Message is documentation"));
            assertTrue(rewritten.contains(version.startsWith("V3")
                    ? "import jakarta.jms.Message;" : "import javax.jms.Message;"));
        }
    }

    @Test void retainsReleaseGatesAndRejectsInvalidContractsAndModels() throws Exception {
        assertFalse(ModelMigration.SUPPORTED_VERSIONS.contains(TARGET));
        for (String version : SOURCES) {
            String original = source(version, false);
            assertFalse(ModelMigration.analyse(original, TARGET).canApply());
            assertThrows(IllegalArgumentException.class, () -> MigrationRules.load(version, TARGET));
            MigrationRules wrongRules = rules(version.equals("V3.3.9") ? "V4.1.6" : "V3.3.9");
            assertThrows(IllegalArgumentException.class, () -> ModelMigration.analyseWithRules(original, wrongRules));
            assertThrows(Exception.class, () -> ModelMigration.analyseWithRules(original + " {}", rules(version)));
            MigrationRules valid = rules(version);
            MigrationRules unsupportedSchema = new MigrationRules(2, version, TARGET, valid.componentMappings(),
                    valid.typePrefixes(), valid.userImportPrefixes(), valid.userImportApplicability());
            assertThrows(IllegalArgumentException.class, () -> ModelMigration.analyseWithRules(original, unsupportedSchema));
            MigrationRules unmapped = new MigrationRules(1, version, TARGET, Map.of(), valid.typePrefixes(),
                    valid.userImportPrefixes(), valid.userImportApplicability());
            assertFalse(ModelMigration.analyseWithRules(original, unmapped).canApply());
            ObjectNode broken = (ObjectNode) StudioJson.newObjectMapper().readTree(original);
            ((ObjectNode) broken.path("flows").get(0).path("consumer")).put("implementingClass", "unknown.Consumer");
            assertFalse(ModelMigration.analyseWithRules(broken.toString(), rules(version)).canApply());
        }
    }
}
