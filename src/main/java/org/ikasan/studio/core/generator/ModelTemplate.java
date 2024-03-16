package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/**
 * Template to create the JSON representation of the module
 */
public class ModelTemplate extends Generator {
    public static final String MODULE_JSON = "module.json";

    public static String create(final Module module) {
        return ComponentIO.toJson(module);
    }
}

