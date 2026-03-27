package org.ikasan.studio.core;

import freemarker.template.Configuration;
import freemarker.template.Version;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static freemarker.template.Configuration.VERSION_2_3_31;


public class BuildContext {
    private static final Map<String, Configuration> configCache = new ConcurrentHashMap<>();
    private static final String FREEMARKER_TEMPLATE_PATH_FORMAT = "/studio/metapack/%s/templates/org/ikasan/studio/generator/";
    public static final String FREEMARKER_OUTPUT_PATH = "/studio/templates/org/ikasan/studio/generator/";

    private BuildContext() {}

    public static Configuration getFreemarkerConfig(String version) {
        return configCache.computeIfAbsent(version, v -> {
            Configuration cfg = new Configuration(VERSION_2_3_31);
            cfg.setClassForTemplateLoading(BuildContext.class, String.format(FREEMARKER_TEMPLATE_PATH_FORMAT, v));
            cfg.setIncompatibleImprovements(new Version(2, 3, 20));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setWhitespaceStripping(true);
            cfg.setLogTemplateExceptions(false);
            return cfg;
        });
    }
}