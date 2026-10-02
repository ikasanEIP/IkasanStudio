package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.persistence.json.IkasanModelExporter;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class IkasanModelExporterTest {
    @TempDir Path parent;

    private org.ikasan.studio.core.model.ikasan.instance.Module module() throws Exception {
        return ComponentIO.validatePersistedModuleJson(Files.readString(Path.of(
                "src/test/resources/org/ikasan/studio/populated_module.json")), "fixture", false);
    }

    @Test void exportsThreeDocumentsWhichReassembleWithoutLosingModelInformation() throws Exception {
        var module = module();
        module.getFlows().get(0).setName("Unsaved renamed flow");
        var mapper = StudioJson.newObjectMapper();
        var expected = mapper.readTree(ModelTemplate.create(module));
        Path output = IkasanModelExporter.export(module, parent, () -> {});
        try (var files = Files.list(output)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactlyInAnyOrder("module.json", "configuration.json", "studio.json");
        }
        var studio = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(output.resolve("studio.json").toFile());
        var reassembled = mapper.createObjectNode();
        reassembled.set("modelFormat", studio.remove("modelFormat"));
        reassembled.set("formatVersion", studio.remove("formatVersion"));
        reassembled.set("studio", studio);
        reassembled.set("module", mapper.readTree(output.resolve("module.json").toFile()));
        reassembled.set("configuration", mapper.readTree(output.resolve("configuration.json").toFile()));
        assertThat(reassembled).isEqualTo(expected);
        var loaded = ComponentIO.validatePersistedModuleJson(reassembled.toString(), "export", false);
        assertThat(loaded.getFlows().get(0).getIdentity()).isEqualTo("Unsaved renamed flow");
        assertThat(mapper.readTree(ModelTemplate.create(module))).isEqualTo(expected);
    }

    @Test void existingExportIsNeverOverwritten() throws Exception {
        Path existing = Files.createDirectory(parent.resolve(IkasanModelExporter.DIRECTORY));
        Files.writeString(existing.resolve("module.json"), "developer content");
        var module = module();
        assertThatThrownBy(() -> IkasanModelExporter.export(module, parent, () -> {}))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readString(existing.resolve("module.json"))).isEqualTo("developer content");
        try (var paths = Files.list(parent)) { assertThat(paths.toList()).containsExactly(existing); }
    }

    @Test void cancellationAfterFirstFileRemovesPartialExport() throws Exception {
        var module = module();
        var checks = new AtomicInteger();
        assertThatThrownBy(() -> IkasanModelExporter.export(module, parent, () -> {
            if (checks.incrementAndGet() == 3) throw new java.util.concurrent.CancellationException();
        })).isInstanceOf(java.util.concurrent.CancellationException.class);
        try (var paths = Files.list(parent)) { assertThat(paths.toList()).isEmpty(); }
    }
}
