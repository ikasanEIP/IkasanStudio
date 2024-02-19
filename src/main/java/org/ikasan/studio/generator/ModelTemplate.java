package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Context;
import org.ikasan.studio.io.ComponentIO;
import org.ikasan.studio.model.ikasan.instance.Module;

/**
 * Template to create the JSON representation of the module
 */
public class ModelTemplate extends Generator {
    public static final String MODULE_JSON = "module.json";

    public static void create(final Project project) {
        Module module = Context.getIkasanModule(project.getName());
        createJsonModelFile(project, ComponentIO.toJson(module));
    }
}

