package org.ikasan.studio.core.metapack.loading;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.model.ComponentTypeMeta;
import org.ikasan.studio.core.metapack.model.IkasanMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.ikasan.studio.core.model.ikasan.instance.Module.DUMB_MODULE_VERSION;

/** Loads and enriches component metadata without any Swing or IntelliJ dependency. */
public final class ComponentLibraryLoader {
    public static final String METAPACK_BASE_DIRECTORY = "studio/metapack";
    private static final Logger LOG = LoggerFactory.getLogger(ComponentLibraryLoader.class);

    public MetaPackManifest loadManifest(String version) throws StudioBuildException {
        return ComponentIO.deserializeResource(
                METAPACK_BASE_DIRECTORY + "/" + version + "/metapack.json", MetaPackManifest.class);
    }

    public Map<String, ComponentMeta> load(String version) {
        Map<String, ComponentMeta> components = new HashMap<>();
        if (version == null || version.isEmpty()) {
            LOG.error("STUDIO: Ikasan metadata-pack version should not be null or empty");
            return components;
        }
        if (DUMB_MODULE_VERSION.equals(version)) {
            LOG.info("STUDIO: Module set to dumb, no metadata pack will be loaded");
            return components;
        }

        String baseDirectory = METAPACK_BASE_DIRECTORY + "/" + version + "/library";
        String[] typeDirectories = subdirectories(baseDirectory);
        if (typeDirectories.length == 0) {
            LOG.error("STUDIO: Metadata pack {} has no component library at {}", version, baseDirectory);
            return components;
        }

        for (String typeDirectory : typeDirectories) {
            ComponentTypeMeta typeMeta;
            try {
                typeMeta = ComponentIO.deserializeComponentTypeMeta(typeDirectory + "/component-type-meta_en_GB.json");
            } catch (StudioBuildException e) {
                LOG.warn("STUDIO: Could not load component type metadata from {}", typeDirectory, e);
                continue;
            }
            for (String componentDirectory : subdirectories(typeDirectory + "/components")) {
                try {
                    IkasanMeta metadata = ComponentIO.deserializeMetaComponent(
                            componentDirectory + "/component-meta_en_GB.json");
                    ComponentMeta component = (ComponentMeta) metadata;
                    enrich(component, typeMeta);
                    component.setIconResourceDirectory(componentDirectory);
                    components.put(component.getName(), component);
                } catch (StudioBuildException e) {
                    LOG.warn("STUDIO: Could not load component metadata from {}", componentDirectory, e);
                }
            }
        }
        return components;
    }

    public String[] subdirectories(String directory) {
        try {
            return ClasspathDirectoryScanner.getDirectories(directory);
        } catch (URISyntaxException | IOException e) {
            LOG.error("STUDIO: Could not scan classpath directory {}", directory, e);
            return new String[0];
        }
    }

    private static void enrich(ComponentMeta component, ComponentTypeMeta type) {
        component.setComponentTypeMeta(type);
        if (type.getJarDependencies() != null) {
            if (component.getJarDependencies() == null) {
                component.setJarDependencies(type.getJarDependencies());
            } else {
                component.getJarDependencies().addAll(type.getJarDependencies());
            }
        }
        if (type.getAllowableProperties() != null) {
            for (Map.Entry<String, ComponentPropertyMeta> property : type.getAllowableProperties().entrySet()) {
                if (component.getAllowableProperties().containsKey(property.getKey())) {
                    LOG.warn("STUDIO: Type property {} overrides component property for {}",
                            property.getKey(), component.getName());
                }
                component.getAllowableProperties().put(property.getKey(), property.getValue());
            }
        }
    }
}
