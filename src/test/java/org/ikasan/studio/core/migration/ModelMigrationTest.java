package org.ikasan.studio.core.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
class ModelMigrationTest {
    private String fixture() throws Exception {
        return Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json"));
    }

    @Test void migratesInBothDirectionsWithoutMutatingTheSource() throws Exception {
        String source = fixture();
        var forward = ModelMigration.analyse(source, "V4.1.6");
        assertThat(forward.canApply()).as(forward.report()).isTrue();
        assertThat(forward.sourceJson()).isEqualTo(source);
        var back = ModelMigration.analyse(forward.targetJson(), "V3.3.9");
        assertThat(back.canApply()).as(back.report()).isTrue();
        assertThat(StudioJson.newObjectMapper().readTree(back.targetJson())).isEqualTo(StudioJson.newObjectMapper().readTree(source));
    }

    @Test void convertsTypePositionsAndRetainsOpaqueExtensionsThroughStudioReload() throws Exception {
        var json = StudioJson.newObjectMapper();
        ObjectNode source = (ObjectNode) json.readTree(fixture());
        source.set("teamSettings", json.readTree("{\"name\":\"javax.jms.Message\",\"values\":[1,true,null]}"));
        var component = (ObjectNode) source.path("flows").get(0).path("flowElements").get(0);
        component.put("fromType", "javax.jms.Message");
        component.set("futureField", json.readTree("[1,2.25,null]"));
        var plan = ModelMigration.analyse(source.toString(), "V4.1.6");
        assertThat(plan.canApply()).as(plan.report()).isTrue();
        var target = json.readTree(plan.targetJson());
        assertThat(target.path("teamSettings")).isEqualTo(source.path("teamSettings"));
        assertThat(target.path("flows").get(0).path("flowElements").get(0).path("fromType").asText()).isEqualTo("jakarta.jms.Message");
        var loaded = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "test", false);
        var reserialized = json.readTree(ComponentIO.toJson(loaded));
        assertThat(reserialized.path("teamSettings")).isEqualTo(source.path("teamSettings"));
        assertThat(reserialized.path("flows").get(0).path("flowElements").get(0).path("futureField")).isEqualTo(component.path("futureField"));
    }

    @Test void rejectsDuplicateKeysAndTrailingJson() throws Exception {
        String source = fixture();
        assertThatThrownBy(() -> ModelMigration.analyse(source + " {}", "V4.1.6")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> ModelMigration.analyse(source.replace("\"version\":\"V3.3.9\"", "\"version\":\"V3.3.9\",\"version\":\"V4.1.6\""), "V4.1.6")).isInstanceOf(Exception.class);
    }

    @Test void blocksUnsupportedPathsUnknownComponentsAndBrokenTransitions() throws Exception {
        assertThat(ModelMigration.analyse(fixture(), "V5.0.0").canApply()).isFalse();
        assertThat(ModelMigration.analyse(fixture().replace("org.ikasan.component.endpoint.consumer.EventGeneratingConsumer", "com.example.Unknown"), "V4.1.6").canApply()).isFalse();
        assertThat(ModelMigration.analyse(fixture().replace("\"to\":\"My Custom Converter\"", "\"to\":\"Missing\""), "V4.1.6").canApply()).isFalse();
    }

    @Test void preservesRoutesAndMigratesExceptionIdentities() throws Exception {
        for (String name : new String[]{"populated_module_with_router.json", "populated_module_with_exception_resolver.json"}) {
            String saved = ComponentIO.toJson(ComponentIO.deserializeModuleInstanceString(
                    Files.readString(Path.of("src/test/resources/org/ikasan/studio", name)), name));
            var forward = ModelMigration.analyse(saved, "V4.1.6");
            assertThat(forward.canApply()).as(forward.report()).isTrue();
            var back = ModelMigration.analyse(forward.targetJson(), "V3.3.9");
            assertThat(back.canApply()).as(back.report()).isTrue();
            assertThat(StudioJson.newObjectMapper().readTree(back.targetJson())).isEqualTo(StudioJson.newObjectMapper().readTree(saved));
        }
    }

    @Test void writesRepresentativeProjectsForMavenSmokeCompilation() throws Exception {
        var json = StudioJson.newObjectMapper();
        ObjectNode source = (ObjectNode) json.readTree(fixture());
        ObjectNode flow = (ObjectNode) source.path("flows").get(0);
        ((com.fasterxml.jackson.databind.node.ArrayNode) flow.path("flowElements")).remove(0);
        flow.set("transitions", json.readTree("[{\"from\":\"My Event Generating Consumer\",\"to\":\"My DevNull Producer\",\"name\":\"default\"}]"));
        String pom = "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>migration-smoke</artifactId><version>1</version>"
                + "<build><sourceDirectory>generated/src/main/java</sourceDirectory><plugins><plugin><artifactId>maven-compiler-plugin</artifactId><version>3.8.0</version></plugin></plugins></build></project>";
        String current = source.toString();
        for (String version : new String[]{"V4.1.6", "V3.3.9"}) {
            var plan = ModelMigration.analyse(current, version);
            assertThat(plan.canApply()).as(plan.report()).isTrue();
            var files = MigrationArtifacts.render(plan, pom);
            Path root = Path.of("build/migration-compile", version);
            for (var entry : files.entrySet()) {
                Path target = root.resolve(entry.getKey());
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue());
                if (entry.getKey().endsWith(".java")) {
                    var declaration = java.util.regex.Pattern.compile("(?m)^public class (\\w+)").matcher(entry.getValue());
                    assertThat(declaration.find()).as(entry.getKey()).isTrue();
                    assertThat(target.getFileName().toString()).isEqualTo(declaration.group(1) + ".java");
                }
            }
            current = plan.targetJson();
            pom = files.get("pom.xml");
        }
    }

    @Test void previewsActualGeneratedArtifactsAndBuildContractInBothDirections() throws Exception {
        String pom = "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>demo</artifactId><version>1</version><properties><team.setting>keep</team.setting></properties></project>";
        var forward = ModelMigration.analyse(fixture(), "V4.1.6");
        var files = MigrationArtifacts.render(forward, pom);
        assertThat(files.get("pom.xml")).contains("<version.ikasan>4.1.6</version.ikasan>", "<maven.compiler.target>17</maven.compiler.target>", "<team.setting>keep</team.setting>");
        assertThat(files.keySet()).noneMatch(path -> path.startsWith("user/"));
        assertThat(files.values()).allMatch(text -> !text.isBlank());
        assertThat(files.get("generated/src/main/java/org/ikasan/studio/boot/Application.java")).contains("class Application");
        var back = MigrationArtifacts.render(ModelMigration.analyse(forward.targetJson(), "V3.3.9"), files.get("pom.xml"));
        assertThat(back.get("pom.xml")).contains("<version.ikasan>3.3.9</version.ikasan>", "<maven.compiler.target>11</maven.compiler.target>");
    }
}
