package org.ikasan.studio.generator;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.Context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FreemarkerUtils {
    private static final Logger LOG = Logger.getInstance("#FreemarkerUtils");

    // Enforce as a utility class
    private FreemarkerUtils() {}

    public static String generateFromTemplate(String templateName, Map<String, Object> configurations) {
        String output = "";
        try (StringWriter writer = new StringWriter()) {
            addSupportForJavaUtilsClasses(configurations);
            Template template = Context.getFeemarkerConfig().getTemplate(templateName);
            template.process(configurations, writer);
            // Would have thought we remove \r in Freemarker but it does not look like it.
            output = writer.toString().replace("\r", "");
        } catch (IOException | TemplateException e) {
            LOG.warn("Problems encountered trying to read template " + templateName + " exception message " + e.getMessage(), e);
        }
        return output;
    }

    private static void addSupportForJavaUtilsClasses(Map<String, Object> configurations) {
        configurations.put("statics", new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_31)
                .build()
                .getStaticModels());
    }
}
