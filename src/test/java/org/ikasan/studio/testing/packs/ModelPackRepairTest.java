package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.ModelPackRepair.Plan;
import org.ikasan.studio.core.metapack.ModelPackRepair;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.*;

class ModelPackRepairTest {
    @TempDir Path directory;
    private final ObjectMapper json = StudioJson.newObjectMapper();

    private ObjectNode document() throws Exception {
        return (ObjectNode) json.readTree(ModelTemplate.create(
                TestFixtures.getMyFirstModuleIkasanModule("V5.1.0", List.of())));
    }

    @Test void revisionRepairChangesOnlyTheLockAndPreservesExactOriginalBackup() throws Exception {
        ObjectNode source = document();
        ((ObjectNode) source.get("studio")).put("metaPackRevision", "0.1.0-SNAPSHOT");
        String original = source.toPrettyString();
        Plan plan = ModelPackRepair.preview(original);
        assertThat(plan.isRevisionUpdate()).isTrue();
        ObjectNode expected = source.deepCopy();
        ((ObjectNode) expected.get("studio")).put("metaPackRevision",
                ComponentLibrary.getMetaPackManifest("V5.1.0").packVersion());
        assertThat(json.readTree(plan.targetJson())).isEqualTo(expected);
        assertThat(source.path("studio").path("metaPackRevision").asText()).isEqualTo("0.1.0-SNAPSHOT");
        Path model = directory.resolve("model.json");
        Files.writeString(model, original);
        ModelPackRepair.apply(model, plan);
        assertThat(Files.readString(directory.resolve("model.json.bak.1"))).isEqualTo(original);
        assertThat(ComponentIO.validatePersistedModuleJson(Files.readString(model), "repaired", false).getVersion()).isEqualTo("V5.1.0");
    }

    @Test void retiredSnapshotRepairChangesOnlyReviewedIdentityFields() throws Exception {
        ObjectNode source = document();
        ((ObjectNode) source.get("studio")).put("metaPack", "V5.0.1-SNAPSHOT").put("metaPackRevision", "0.1.0-SNAPSHOT");
        ((ObjectNode) source.get("module")).put("ikasanVersion", "5.0.1-SNAPSHOT");
        ObjectNode expected = document();
        Plan plan = ModelPackRepair.preview(source.toString());
        assertThat(plan.isRevisionUpdate()).isFalse();
        assertThat(plan.previousRuntime()).isEqualTo("5.0.1-SNAPSHOT");
        assertThat(plan.targetRuntime()).isEqualTo("5.1.0");
        assertThat(plan.javaVersion()).isEqualTo("25");
        assertThat(json.readTree(plan.targetJson())).isEqualTo(expected);
    }

    @Test void rejectsHealthyUnknownOrAmbiguousRuntimeWithoutGuessing() throws Exception {
        ObjectNode source = document();
        assertThatThrownBy(() -> ModelPackRepair.preview(source.toString())).hasMessageContaining("already selects");
        ((ObjectNode) source.get("studio")).put("metaPack", "UnknownCustomerPack");
        assertThatThrownBy(() -> ModelPackRepair.preview(source.toString())).isInstanceOf(Exception.class);
        ((ObjectNode) source.get("studio")).put("metaPack", "V5.1.0");
        ((ObjectNode) source.get("module")).put("ikasanVersion", "4.1.6");
        assertThatThrownBy(() -> ModelPackRepair.preview(source.toString())).hasMessageContaining("disagree");
    }

    @Test void rejectsInvalidModelEvenWhenRevisionCouldBeChanged() throws Exception {
        ObjectNode source = document();
        ((ObjectNode) source.get("studio")).put("metaPackRevision", "old");
        ((ObjectNode) source.get("module")).remove("flows");
        assertThatThrownBy(() -> ModelPackRepair.preview(source.toString())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> ModelPackRepair.preview("{broken")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> ModelPackRepair.preview(document() + " {}" )).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> ModelPackRepair.preview("{\"studio\":{},\"studio\":{}}" )).isInstanceOf(Exception.class);
    }

    @Test void staleAndForgedPlansCannotOverwriteTheModel() throws Exception {
        ObjectNode source = document();
        ((ObjectNode) source.get("studio")).put("metaPackRevision", "old");
        Plan plan = ModelPackRepair.preview(source.toString());
        Path model = directory.resolve("model.json");
        Files.writeString(model, source + "\n");
        assertThatThrownBy(() -> ModelPackRepair.apply(model, plan)).hasMessageContaining("changed after review");
        assertThat(Files.exists(directory.resolve("model.json.bak.1"))).isFalse();
        Files.writeString(model, plan.sourceJson());
        Plan forged = new ModelPackRepair.Plan(plan.previousPack(), plan.targetPack(), plan.previousRevision(), plan.targetRevision(),
                plan.previousRuntime(), plan.targetRuntime(), plan.javaVersion(), plan.sourceJson(), "{}");
        assertThatThrownBy(() -> ModelPackRepair.apply(model, forged)).hasMessageContaining("review changed");
        assertThat(Files.readString(model)).isEqualTo(plan.sourceJson());
        assertThat(Files.exists(directory.resolve("model.json.bak.1"))).isFalse();
    }
}
