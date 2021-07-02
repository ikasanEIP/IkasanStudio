package org.ikasan.studio;

import com.intellij.openapi.project.Project;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.ikasan.studio.model.ikasan.IkasanPomModel;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The Context allows all the independent parts of the UI to collaborate with each other.
 *
 * Anything can be placed in the Context, some things are so important that they will have their own getters
 * just for convenience.
 */
public enum Context {
    INSTANCE;

    private final String CANVAS_PANEL = "canvasPanel";
    private final String PROJECT = "project";
    private final String APPLICATION_PROPERTIES = "applicationProperties";
    private final String OPTIONS = "options";
    private final String PROPERTIES_PANEL = "propertiesPanel";
    private final String PROPERTIES_AND_CANVAS_SPLITPANE = "propertiesAndCanvasSplitPane";
    private final String CANVAS_TEXT_AREA = "canvasTextArea";
    private final String IKASAN_MODULE = "ikasanModule";
    private final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";
    private final String POM = "pom";

    Context() {
        freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setClassForTemplateLoading(Context.class, FREEMARKER_TEMPLATE_PATH);
        freemarkerConfiguration.setIncompatibleImprovements(new Version(2, 3, 20));
        freemarkerConfiguration.setDefaultEncoding("UTF-8");
        freemarkerConfiguration.setWhitespaceStripping(true);
    }

    // projectName -> region -> value
    // e.g. myProject -> INSTANCE.IKASAN_MODULE -> actualModule
    //      myProject -> INSTANCE.PIPSI_IKASAN_MODEL -> actualPipsoModule
    //      myProject -> INSTANCE.PROJECT -> intellijProjectConfigurationData
    private static Map<String, Map<String, Object>> perProjectCache = new HashMap<>();

    public static final String FREEMARKER_TEMPLATE_PATH = "/studio/templates/org/ikasan/studio/generator/";
    private Configuration freemarkerConfiguration ;

    public static Configuration getFeemarkerConfig() {
        return INSTANCE.freemarkerConfiguration;
    }

    private static synchronized void putProjectCache(String projectKey, String key, Object value) {
        perProjectCache.putIfAbsent(projectKey, new HashMap<>());
        perProjectCache.get(projectKey).put(key, value);

        // We should always make sure the options cache is available.
        perProjectCache.get(projectKey).putIfAbsent(INSTANCE.OPTIONS, new Options());

        // Currently hardcode these options but in future we need to expose via IDE

    }

    private static synchronized Object getProjectCache(String projectKey, String key) {
        Map<String, Object> cache = perProjectCache.get(projectKey);
        if (cache != null) {
            return cache.get(key);
        }
        return null;
    }

    public static Options getOption(String projectKey) {
        return (Options)getProjectCache(projectKey, INSTANCE.OPTIONS);
    }

    public static IkasanPomModel getPom(String projectKey) {
        return (IkasanPomModel)getProjectCache(projectKey, INSTANCE.POM);
    }

    public static void setPom(String projectKey, IkasanPomModel pom) {
        putProjectCache(projectKey, INSTANCE.POM, pom);
    }

    public static Project getProject(String projectKey) {
        return (Project)getProjectCache(projectKey, INSTANCE.PROJECT);
    }

    public static void setProject(String projectKey, Project project) {
        putProjectCache(projectKey, INSTANCE.PROJECT, project);
    }


    public static void setApplicationProperties(String projectKey, Properties properties) {
        putProjectCache(projectKey, INSTANCE.APPLICATION_PROPERTIES, properties);
    }

    public static Properties getApplicationProperties(String projectKey) {
        return (Properties) getProjectCache(projectKey, INSTANCE.APPLICATION_PROPERTIES);
    }

    public static void setDesignerCanvas(String projectKey, DesignerCanvas designerCanvas) {
        putProjectCache(projectKey, INSTANCE.CANVAS_PANEL, designerCanvas);
    }

    public static DesignerCanvas getDesignerCanvas(String projectKey) {
        return (DesignerCanvas) getProjectCache(projectKey, INSTANCE.CANVAS_PANEL);
    }

    public static void setPropertiesAndCanvasPane(String projectKey, JSplitPane propertiesAndCanvasPane) {
        putProjectCache(projectKey, INSTANCE.PROPERTIES_AND_CANVAS_SPLITPANE, propertiesAndCanvasPane);
    }

    public static JSplitPane getPropertiesAndCanvasPane(String projectKey) {
        return (JSplitPane) getProjectCache(projectKey, INSTANCE.PROPERTIES_AND_CANVAS_SPLITPANE);
    }

    public static void setPropertiesPanel(String projectKey, ComponentPropertiesPanel componentPropertiesPanel) {
        putProjectCache(projectKey, INSTANCE.PROPERTIES_PANEL, componentPropertiesPanel);
    }
    public static ComponentPropertiesPanel getPropertiesPanel(String projectKey) {
        return (ComponentPropertiesPanel) getProjectCache(projectKey, INSTANCE.PROPERTIES_PANEL);
    }

    public static void setCanvasTextArea(String projectKey, JTextArea canvasTextArea) {
        putProjectCache(projectKey, INSTANCE.CANVAS_TEXT_AREA, canvasTextArea);
    }
    public static JTextArea getCanvasTextArea(String projectKey) {
        return (JTextArea) getProjectCache(projectKey, INSTANCE.CANVAS_TEXT_AREA);
    }

    public static void setIkasanModule(String projectKey, IkasanModule ikasanModule) {
        putProjectCache(projectKey, INSTANCE.IKASAN_MODULE, ikasanModule);
    }
    public static IkasanModule getIkasanModule(String projectKey) {
        return (IkasanModule) getProjectCache(projectKey, INSTANCE.IKASAN_MODULE);
    }
    public static void setPipsiIkasanModel(String projectKey, PIPSIIkasanModel ikasanModule) {
        putProjectCache(projectKey, INSTANCE.PIPSI_IKASAN_MODEL, ikasanModule);
    }
    public static PIPSIIkasanModel getPipsiIkasanModel(String projectKey) {
        return (PIPSIIkasanModel) getProjectCache(projectKey, INSTANCE.PIPSI_IKASAN_MODEL);
    }
}