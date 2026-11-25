package org.ikasan.studio.core.importer;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.ikasan.studio.core.importer.SourceBuilderSettingsRecovery.Plan;
import org.ikasan.studio.core.importer.SourceModuleImport.Analysis;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class SourceBuilderSettingsRecoveryTest {
    @TempDir Path root;
    Path source() { return root.resolve("original"); }
    Path target() { return root.resolve("destination"); }
    String factory;
    void write(Path base, String path, String value) throws Exception {
        Path file = base.resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file, value);
    }
    void fixture(String pack, String argument, String existing) throws Exception {
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(pack);
        Flow flow = TestFixtures.getUnbuiltFlow(pack).consumer(consumer).build();
        consumer.setContainingFlow(flow);
        FlowElement xml = FlowElement.flowElementBuilder().componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack,
                "Object To XML String Converter")).componentName("Serialize payload").build();
        xml.setContainingFlow(flow); xml.setContainingFlowRoute(flow.getFlowRoute());
        if (existing != null) xml.setPropertyValue("objectClass", existing);
        FlowElement producer = TestFixtures.getLoggingProducer(pack);
        producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().setFlowElements(new ArrayList<>(List.of(xml, producer)));
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        String pkg = Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
        factory = "generated/src/main/java/" + pkg.replace('.', '/') + "/ComponentFactory" + flow.getJavaClassName() + ".java";
        write(target(), ProjectArtifactRenderer.MODEL, ModelTemplate.create(module));
        write(target(), factory, FlowsComponentFactoryTemplate.create(pkg, module, flow));
        write(target(), "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>test</artifactId><version>1</version></project>");
        write(source(), "src/main/resources/application.properties", "module.name=" + module.getIdentity() + "\n");
        write(source(), "src/main/java/customer/Payload.java", "package customer; public class Payload {}");
        write(source(), factory, "package " + pkg + "; import customer.Payload; public class ComponentFactory" + flow.getJavaClassName()
                + " { public Object get" + xml.getJavaClassName() + "() { return builderFactory.getComponentBuilder().objectToXmlStringConverter().setObjectClass("
                + argument + ").build(); } }");
    }
    SourceModuleImport.Analysis analyse() throws Exception { return SourceModuleImport.analyse(source(), target(), () -> { }); }

    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6", "V5.1.0"})
    void recoversLiteralClassThroughMetaPackAndPersistsItWithBackup(String pack) throws Exception {
        fixture(pack, "Payload.class", null);
        Analysis analysis = analyse(); Plan plan = SourceBuilderSettingsRecovery.preview(analysis);
        assertNotNull(plan, "Expected a recovery plan for the class literal");
        assertThat(plan.settings()).hasSize(1);
        assertThat(plan.settings().get(0).value()).isEqualTo("customer.Payload");
        assertThat(plan.outputs().get(factory)).contains(".setObjectClass(customer.Payload.class)");
        String before = Files.readString(target().resolve(ProjectArtifactRenderer.MODEL));
        assertThat(SourceBuilderSettingsRecovery.preview(analysis)).isEqualTo(plan);
        // Only the explicit plan authorises model/factory changes; selecting source files alone does not.
        SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true);
        assertThat(Files.readString(target().resolve(ProjectArtifactRenderer.MODEL))).isEqualTo(before);
        SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true, Map.of(), null, plan);
        Module restored = ComponentIO.validatePersistedModuleJson(Files.readString(target().resolve(ProjectArtifactRenderer.MODEL)), "test", false);
        assertThat(restored.getFlows().get(0).getFlowElementsNoExternalEndPoints().stream()
                .filter(e -> e.getComponentName().equals("Serialize payload")).findFirst().orElseThrow().getPropertyValueAsString("objectClass"))
                .isEqualTo("customer.Payload");
        try (Stream<Path> files = Files.list(target().resolve("user/import-reviews"))) {
            Path backup = files.filter(p -> p.getFileName().toString().startsWith("model-before-builder-recovery-")).findFirst().orElseThrow();
            assertThat(Files.readString(backup)).isEqualTo(before);
        }
        assertThat(SourceBuilderSettingsRecovery.preview(analyse())).isNull();
    }

    @Test void preservesExistingValuesAndDeclinesComputedArguments() throws Exception {
        fixture("V3.3.9", "Payload.class", "java.lang.String");
        assertThat(SourceBuilderSettingsRecovery.preview(analyse())).isNull();
        fixture("V3.3.9", "resolvePayloadClass()", null);
        assertThat(SourceBuilderSettingsRecovery.preview(analyse())).isNull();
    }

    @Test void rejectsStaleAndForgedPlansWithoutWritingAnything() throws Exception {
        fixture("V3.3.9", "Payload.class", null);
        Analysis analysis = analyse(); Plan plan = SourceBuilderSettingsRecovery.preview(analysis);
        assertNotNull(plan, "Expected a recovery plan before editing the source");
        String before = Files.readString(target().resolve(ProjectArtifactRenderer.MODEL));
        Plan forged = new SourceBuilderSettingsRecovery.Plan(plan.settings(), plan.sourceInputs(), plan.destinationInputs(), Map.of("user/Bad.java", "bad"));
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true, Map.of(), null, forged))
                .hasMessageContaining("changed since review");
        write(source(), factory, Files.readString(source().resolve(factory)).replace("Payload.class", "String.class"));
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true, Map.of(), null, plan))
                .hasMessageContaining("changed since review");
        assertThat(Files.readString(target().resolve(ProjectArtifactRenderer.MODEL))).isEqualTo(before);
        assertThat(target().resolve("user/Bad.java")).doesNotExist();
    }

    @Test void destinationEditsInvalidateTheReviewedPlan() throws Exception {
        fixture("V3.3.9", "Payload.class", null);
        Analysis analysis = analyse(); Plan plan = SourceBuilderSettingsRecovery.preview(analysis);
        String before = Files.readString(target().resolve(ProjectArtifactRenderer.MODEL));
        write(target(), factory, Files.readString(target().resolve(factory)) + "\n// changed during review");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true, Map.of(), null, plan))
                .hasMessageContaining("changed since review");
        assertThat(Files.readString(target().resolve(ProjectArtifactRenderer.MODEL))).isEqualTo(before);
    }

    @Test void acceptsFullyQualifiedClassLiteralsWithoutImports() throws Exception {
        fixture("V3.3.9", "customer.Payload.class", null);
        write(source(), factory, Files.readString(source().resolve(factory)).replace("import customer.Payload;", ""));
        Plan plan = SourceBuilderSettingsRecovery.preview(analyse());
        assertNotNull(plan, "Expected a recovery plan for the fully qualified class literal");
        assertThat(plan.settings().get(0).value()).isEqualTo("customer.Payload");
    }

    @Test void duplicateSettersAndWildcardImportsAreNotGuessed() throws Exception {
        fixture("V3.3.9", "Payload.class", null);
        write(source(), factory, Files.readString(source().resolve(factory)).replace(".build()", ".setObjectClass(java.lang.String.class).build()"));
        assertThat(SourceBuilderSettingsRecovery.preview(analyse())).isNull();
        fixture("V3.3.9", "Payload.class", null);
        write(source(), factory, Files.readString(source().resolve(factory)).replace("import customer.Payload", "import customer.*"));
        assertThat(SourceBuilderSettingsRecovery.preview(analyse())).isNull();
    }
}
