package org.ikasan.studio.core.model;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModelUtils.class);
    public static Module generateModuleInstanceFromString(String json, String path) {
        Module newModule;
        try {
            newModule = ComponentIO.deserializeModuleInstanceString(json, path);
        } catch (StudioBuildException e) {
            LOG.warn("Could not read module json");
            throw new RuntimeException(e);
        }
        return newModule;
    }
}
