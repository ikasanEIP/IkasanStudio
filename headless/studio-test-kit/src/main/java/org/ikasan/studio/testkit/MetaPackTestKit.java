package org.ikasan.studio.testkit;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.loading.ComponentLibraryLoader;
import org.ikasan.studio.core.metapack.validation.MetaPackValidator;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Headless operations for pack authors. Packs are resources on the caller's application classpath. */
public final class MetaPackTestKit {
    private MetaPackTestKit() { }

    public static void validatePack(String packId) throws StudioBuildException {
        ComponentLibraryLoader loader = new ComponentLibraryLoader();
        loader.validateSources(packId);
        MetaPackValidator.validate(packId, loader.loadManifest(packId), loader.load(packId));
    }

    public static Module readModel(String json, String source) throws StudioBuildException {
        return ComponentIO.validatePersistedModuleJson(json, source, false);
    }

    /**
     * Renders shared module and flow artifacts without writing files or launching an IDE.
     * Keys are logical artifact names, not filesystem paths. User stubs/recipes can be tested separately
     * through the public generator APIs with the author's own component fixtures.
     */
    public static Map<String, String> render(Module module) throws StudioBuildException, StudioGeneratorException {
        validatePack(module.getVersion());
        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put("model.json", ModelTemplate.create(module));
        artifacts.put("Application.java", ApplicationTemplate.create(module));
        artifacts.put("ModuleConfig.java", ModuleConfigTemplate.create(module));
        artifacts.put("application.properties", PropertiesTemplate.create(module));
        int index = 0;
        for (var flow : module.getFlows()) {
            String prefix = "flow[" + index++ + "]/";
            artifacts.put(prefix + "Flow.java", FlowTemplate.create(module.getApplicationPackageName(), module, flow));
            artifacts.put(prefix + "ComponentFactory.java",
                    FlowsComponentFactoryTemplate.create(module.getApplicationPackageName(), module, flow));
        }
        return Collections.unmodifiableMap(artifacts);
    }
}
