package org.ikasan.studio.core.generator;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.ikasan.studio.core.BuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FreemarkerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FreemarkerUtils.class);

    // Enforce as a utility class
    private FreemarkerUtils() {}

    public static String generateFromTemplate(String templateName, Map<String, Object> configurations) throws StudioGeneratorException {
        String output;
        try (StringWriter writer = new StringWriter()) {
            addSupportForJavaUtilsClasses(configurations);
            Template template = BuildContext.getFeemarkerConfig().getTemplate(templateName);
            template.process(configurations, writer);
            // Would have thought we remove \r in Freemarker but it does not look like it.
            output = writer.toString().replace("\r", "");
        } catch (IOException | TemplateException e) {
            LOG.warn("STUDIO: SERIOUS: Problems encountered trying to generate template " + templateName + " exception message " + e.getMessage(), e);
            throw new StudioGeneratorException("STUDIO: Serious: Problems encountered trying to generate template " + templateName + ", please check the logs");
        }
        return output;
    }

    private static void addSupportForJavaUtilsClasses(Map<String, Object> configurations) {
        configurations.put("statics", new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_31)
                .build()
                .getStaticModels());
    }
}
