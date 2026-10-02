package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

/** Exports a complete model snapshot without changing the project's authoritative model.json. */
public final class IkasanModelExporter {
    public static final String DIRECTORY = "ikasan-model-documents";

    private IkasanModelExporter() {}

    /**
     * Writes module.json, configuration.json and studio.json into a new child directory.
     * The caller must keep the module stable for the duration of this operation and run it off the UI thread.
     * Existing destinations are rejected; cancelled or failed writes remove the staging directory.
     * The Studio document includes the container's format identifier and version for future reassembly.
     */
    public static Path export(Module module, Path parent, Runnable checkCancelled) throws IOException {
        checkCancelled.run();
        var mapper = StudioJson.newObjectMapper();
        var documents = mapper.readTree(ModelTemplate.create(module));
        ObjectNode studio = documents.path("studio").deepCopy();
        studio.set("modelFormat", documents.get("modelFormat"));
        studio.set("formatVersion", documents.get("formatVersion"));
        Path target = parent.resolve(DIRECTORY);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(target.toString());
        Path staging = Files.createTempDirectory(parent, ".ikasan-model-export-");
        try {
            for (String name : List.of("module", "configuration", "studio")) {
                checkCancelled.run();
                var document = name.equals("studio") ? studio : documents.get(name);
                Files.writeString(staging.resolve(name + ".json"),
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document) + "\n",
                        StandardOpenOption.CREATE_NEW);
            }
            checkCancelled.run();
            Files.move(staging, target);
            return target;
        } finally {
            if (Files.exists(staging)) {
                try (var paths = Files.walk(staging)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            }
        }
    }
}
