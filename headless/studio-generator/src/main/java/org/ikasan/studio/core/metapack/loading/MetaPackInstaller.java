package org.ikasan.studio.core.metapack.loading;

import org.ikasan.studio.core.BuildContext;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.metapack.validation.MetaPackValidator;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import freemarker.template.Configuration;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/** Validates a private copy, then publishes it for the next IDE session. Existing IDs are never replaced. */
public final class MetaPackInstaller {
    private MetaPackInstaller() { }

    public static MetaPackManifest validate(MetaPackArchive archive) throws Exception {
        return MetaPackResources.inspect(archive, () -> {
            ComponentLibraryLoader loader = new ComponentLibraryLoader();
            MetaPackManifest manifest = loader.loadManifest(archive.id());
            if (manifest.schemaVersion() != 2) throw new StudioBuildException("External packs require manifest schemaVersion 2");
            loader.validateSources(archive.id());
            Map<String,ComponentMeta> components = loader.load(archive.id());
            MetaPackValidator.validate(archive.id(), manifest, components);
            if (!components.keySet().containsAll(Set.of(ComponentMeta.MODULE_TYPE, ComponentMeta.FLOW_TYPE,
                    ComponentMeta.EXCEPTION_RESOLVER_TYPE))) {
                throw new StudioBuildException("External packs must include Module, Flow and Exception Resolver components");
            }
            // Parse every template, including includes not reached by the minimal rendering contract.
            Configuration configuration = BuildContext.getFreemarkerConfig(archive.id());
            String prefix = "studio/metapack/" + archive.id() + "/templates/org/ikasan/studio/generator/";
            for (String template : archive.templateNames(prefix)) configuration.getTemplate(template);
            for (String required : Set.of("applicationTemplate_en.ftl", "moduleConfigTemplate_en.ftl",
                    "flowTemplate_en.ftl", "componentFactory_en.ftl", "propertiesTemplate_en.ftl")) {
                configuration.getTemplate(required);
            }
            return manifest;
        });
    }

    public static synchronized MetaPackManifest install(Path source, Path directory, Runnable checkCancelled) throws Exception {
        return install(source, directory, checkCancelled, null);
    }

    public static MetaPackManifest install(Path source, Path directory, Runnable checkCancelled, MetaPackStore.Candidate reviewed) throws Exception {
        MetaPackResources.initialize();
        return MetaPackStore.locked(directory, () -> installLocked(source, directory, checkCancelled, reviewed));
    }

    private static MetaPackManifest installLocked(Path source, Path directory, Runnable checkCancelled, MetaPackStore.Candidate reviewed) throws Exception {
        Path staged = Files.createTempFile(directory, ".install-", ".tmp");
        try {
            checkCancelled.run();
            Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
            MetaPackArchive archive = MetaPackArchive.open(staged);
            MetaPackManifest manifest = validate(archive);
            if (reviewed != null && (!manifest.equals(reviewed.manifest()) || !MetaPackStore.sha256(staged).equals(reviewed.sha256())))
                throw new IOException("The candidate changed after review; review again");
            MetaPackStore.requireImmutableRevision(directory, manifest, MetaPackStore.sha256(staged));
            if (MetaPackResources.resource("studio/metapack/" + archive.id() + "/metapack.json") != null) {
                throw new IOException("Pack ID " + archive.id() + " is already available; existing packs cannot be replaced");
            }
            // Also inspect pending installations, which are deliberately absent from the session-fixed index.
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                    if (MetaPackArchive.open(file).id().equals(archive.id())) {
                        throw new IOException("Pack ID " + archive.id() + " is already installed; restart IntelliJ IDEA to use it");
                    }
                }
            }
            checkCancelled.run();
            // Same-filesystem move publishes only the fully validated file. No REPLACE_EXISTING.
            Path installationDirectory = directory.toAbsolutePath().normalize();
            Path destination = installationDirectory.resolve(archive.id() + ".jar").normalize();
            if (!destination.startsWith(installationDirectory) || !installationDirectory.equals(destination.getParent())) {
                throw new IOException("Pack destination must be directly inside the installation directory");
            }
            Files.move(staged, destination);
            return manifest;
        } finally { Files.deleteIfExists(staged); }
    }
}
