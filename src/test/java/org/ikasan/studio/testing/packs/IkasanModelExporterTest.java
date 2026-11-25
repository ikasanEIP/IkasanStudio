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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class IkasanModelExporterTest {
    @TempDir Path parent;

    private org.ikasan.studio.core.model.ikasan.instance.Module module() throws Exception {
        return ComponentIO.validatePersistedModuleJson(Files.readString(Path.of(
                "src/test/resources/org/ikasan/studio/populated_module.json")), "fixture", false);
    }

    @Test void exportsThreeDocumentsWhichReassembleWithoutLosingModelInformation() throws Exception {
        Module module = module();
        module.getFlows().get(0).setName("Unsaved renamed flow");
        ObjectMapper mapper = StudioJson.newObjectMapper();
        JsonNode expected = mapper.readTree(ModelTemplate.create(module));
        Path output = IkasanModelExporter.export(module, parent, () -> {});
        try (Stream<Path> files = Files.list(output)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactlyInAnyOrder("module.json", "configuration.json", "studio.json");
        }
        ObjectNode studio = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(output.resolve("studio.json").toFile());
        ObjectNode reassembled = mapper.createObjectNode();
        reassembled.set("modelFormat", studio.remove("modelFormat"));
        reassembled.set("formatVersion", studio.remove("formatVersion"));
        reassembled.set("studio", studio);
        reassembled.set("module", mapper.readTree(output.resolve("module.json").toFile()));
        reassembled.set("configuration", mapper.readTree(output.resolve("configuration.json").toFile()));
        assertThat(reassembled).isEqualTo(expected);
        Module loaded = ComponentIO.validatePersistedModuleJson(reassembled.toString(), "export", false);
        assertThat(loaded.getFlows().get(0).getIdentity()).isEqualTo("Unsaved renamed flow");
        assertThat(mapper.readTree(ModelTemplate.create(module))).isEqualTo(expected);
    }

    @Test void existingExportIsNeverOverwritten() throws Exception {
        Path existing = Files.createDirectory(parent.resolve(IkasanModelExporter.DIRECTORY));
        Files.writeString(existing.resolve("module.json"), "developer content");
        Module module = module();
        assertThatThrownBy(() -> IkasanModelExporter.export(module, parent, () -> {}))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readString(existing.resolve("module.json"))).isEqualTo("developer content");
        try (Stream<Path> paths = Files.list(parent)) { assertThat(paths.toList()).containsExactly(existing); }
    }

    @Test void cancellationAfterFirstFileRemovesPartialExport() throws Exception {
        Module module = module();
        AtomicInteger checks = new AtomicInteger();
        assertThatThrownBy(() -> IkasanModelExporter.export(module, parent, () -> {
            if (checks.incrementAndGet() == 3) throw new java.util.concurrent.CancellationException();
        })).isInstanceOf(java.util.concurrent.CancellationException.class);
        try (Stream<Path> paths = Files.list(parent)) { assertThat(paths.toList()).isEmpty(); }
    }
}
