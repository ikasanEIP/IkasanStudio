package org.ikasan.studio;

import freemarker.template.Configuration;
import freemarker.template.Version;

public enum CContext {
    INSTANCE;
//    private static final Configuration ;

    CContext() {
        freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setClassForTemplateLoading(CContext.class, FREEMARKER_TEMPLATE_PATH);
        freemarkerConfiguration.setIncompatibleImprovements(new Version(2, 3, 20));
        freemarkerConfiguration.setDefaultEncoding("UTF-8");
        freemarkerConfiguration.setWhitespaceStripping(true);
    }

    private final String CANVAS_PANEL = "canvasPanel";
    private final String PROJECT = "project";
    private final String APPLICATION_PROPERTIES = "applicationProperties";
    private final String OPTIONS = "options";
    private final String PROPERTIES_PANEL = "propertiesPanel";
    private final String PROPERTIES_AND_CANVAS_SPLITPANE = "propertiesAndCanvasSplitPane";
    private final String CANVAS_TEXT_AREA = "canvasTextArea";
    private final String IKASAN_MODULE = "ikasanModule";
    private final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";
    public static final String FREEMARKER_TEMPLATE_PATH = "/studio/templates/org/ikasan/studio/generator/";
    private Configuration freemarkerConfiguration ;

    public static Configuration getFeemarkerConfig() {
        return INSTANCE.freemarkerConfiguration;
    }
}
