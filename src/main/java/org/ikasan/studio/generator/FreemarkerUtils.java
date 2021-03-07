package org.ikasan.studio.generator;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.ikasan.studio.CContext;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FreemarkerUtils {
    private static final Logger log = Logger.getLogger(FreemarkerUtils.class);

    public static String generateFromTemplate(String templateName, Map<String, Object> configurations) {
        String output = "";
        try (StringWriter writer = new StringWriter()) {

            Template template = CContext.getFeemarkerConfig().getTemplate(templateName);
            template.process(configurations, writer);
            // Would have thought we remove \r in Freemarker but it does not look like it.
            output = writer.toString().replaceAll("\r", "");
        } catch (IOException | TemplateException e) {
            log.warn("Problems encountered trying to read template " + templateName + " exception message " + e.getMessage(), e);
        }
        return output;
    }
}
