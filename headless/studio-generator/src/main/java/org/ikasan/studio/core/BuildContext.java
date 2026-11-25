package org.ikasan.studio.core;

import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.ikasan.studio.core.metapack.loading.MetaPackResources;

import static freemarker.template.Configuration.VERSION_2_3_31;


public class BuildContext {
    private static final String FREEMARKER_TEMPLATE_PATH_FORMAT = "/studio/metapack/%s/templates/org/ikasan/studio/generator/";
    public static final String FREEMARKER_OUTPUT_PATH = "/studio/templates/org/ikasan/studio/generator/";

    private BuildContext() {}

    public static Configuration getFreemarkerConfig(String version) {
        Configuration cfg = new Configuration(VERSION_2_3_31);
            cfg.setClassLoaderForTemplateLoading(MetaPackResources.templateLoader(version), String.format(FREEMARKER_TEMPLATE_PATH_FORMAT, version));
            // Release inspected JAR handles promptly, including on Windows before publishing a staged pack.
            ((URLTemplateLoader) cfg.getTemplateLoader()).setURLConnectionUsesCaches(false);
            cfg.setIncompatibleImprovements(new Version(2, 3, 20));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setWhitespaceStripping(true);
            cfg.setLogTemplateExceptions(false);
        return cfg;
    }
}
