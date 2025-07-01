package org.ikasan.studio.core;

import freemarker.template.Configuration;
import freemarker.template.Version;

import static freemarker.template.Configuration.VERSION_2_3_31;


public enum BuildContext {
    INSTANCE;
    BuildContext() {
        freemarkerConfiguration = new Configuration(VERSION_2_3_31);
        freemarkerConfiguration.setClassForTemplateLoading(BuildContext.class, FREEMARKER_TEMPLATE_PATH);
        freemarkerConfiguration.setIncompatibleImprovements(new Version(2, 3, 20));
        freemarkerConfiguration.setDefaultEncoding("UTF-8");
        freemarkerConfiguration.setWhitespaceStripping(true);
        freemarkerConfiguration.setLogTemplateExceptions(false);
    }

    // @todo needs to be version configurable
    public static final String FREEMARKER_TEMPLATE_PATH = "/studio/metapack/V3.3.3/templates/org/ikasan/studio/generator/";
    public static final String FREEMARKER_OUTPUT_PATH = "/studio/templates/org/ikasan/studio/generator/";
    private final Configuration freemarkerConfiguration ;

    public static Configuration getFeemarkerConfig() {
        return INSTANCE.freemarkerConfiguration;
    }
}
