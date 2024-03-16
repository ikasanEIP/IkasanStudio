package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Options;
import org.ikasan.studio.build.model.ikasan.IkasanPomModel;
import org.ikasan.studio.build.model.ikasan.instance.Module;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

import javax.swing.*;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The Context allows all the independent parts of the UI to collaborate with each other.
 * Anything can be placed in the Context, some things are so important that they will have their own getters
 * just for convenience.
 */
public enum Context {
    INSTANCE;
    public static final String JAVA_FILE_EXTENSION = "java";
    public static final String JSON_FILE_EXTENSION = "json";
    public static final String XML_FILE_EXTENSION = "xml";

    private static final String CANVAS_PANEL = "canvasPanel";
    private static final String PROJECT = "project";
    private static final String APPLICATION_PROPERTIES = "applicationProperties";
    private static final String OPTIONS = "options";
    private static final String PROPERTIES_PANEL = "propertiesPanel";
    private static final String PROPERTIES_AND_CANVAS_SPLITPANE = "propertiesAndCanvasSplitPane";
    private static final String CANVAS_TEXT_AREA = "canvasTextArea";
    private static final String IKASAN_MODULE = "ikasanModule";
    private static final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";
    private static final String POM = "pom";

    public static final String JSON_MODEL_PARENT_DIR = "main";
    public static final String JSON_MODEL_SUB_DIR = "model";
    public static final String JSON_MODEL_DIR =
            JSON_MODEL_PARENT_DIR + FileSystems.getDefault().getSeparator() +
                    JSON_MODEL_SUB_DIR;
    public static final String JSON_MODEL_FILE_WITH_EXTENSION = "model"  + "." + JSON_FILE_EXTENSION;
    public static final String JSON_MODEL_FULL_PATH =
            JSON_MODEL_DIR +  FileSystems.getDefault().getSeparator() +
            JSON_MODEL_FILE_WITH_EXTENSION;

    // projectName -> region -> value
    // e.g. myProject -> INSTANCE.IKASAN_MODULE -> actualModule
    //      myProject -> INSTANCE.PIPSI_IKASAN_MODEL -> actualPipsoModule
    //      myProject -> INSTANCE.PROJECT -> intellijProjectConfigurationData
    private static final Map<String, Map<String, Object>> perProjectCache = new HashMap<>();

    private static synchronized void putProjectCache(String projectKey, String key, Object value) {
        perProjectCache.putIfAbsent(projectKey, new TreeMap<>());
        perProjectCache.get(projectKey).put(key, value);
        // We should always make sure the options cache is available.
        perProjectCache.get(projectKey).putIfAbsent(OPTIONS, new Options());
        // Currently hardcode these options but in future we need to expose via IDE
    }

    private static synchronized Object getProjectCache(String projectKey, String key) {
        Map<String, Object> cache = perProjectCache.get(projectKey);
        if (cache != null) {
            return cache.get(key);
        }
        return null;
    }
    public static Options getOptions(String projectKey) {
        return (Options)getProjectCache(projectKey, OPTIONS);
    }

    public static IkasanPomModel getPom(String projectKey) {
        return (IkasanPomModel)getProjectCache(projectKey, POM);
    }

    public static void setPom(String projectKey, IkasanPomModel pom) {
        putProjectCache(projectKey, POM, pom);
    }

    public static Project getProject(String projectKey) {
        return (Project)getProjectCache(projectKey, PROJECT);
    }

    public static void setProject(String projectKey, Project project) {
        putProjectCache(projectKey, PROJECT, project);
    }

    public static void setApplicationProperties(String projectKey, Properties properties) {
        putProjectCache(projectKey, APPLICATION_PROPERTIES, properties);
    }

    public static Properties getApplicationProperties(String projectKey) {
        return (Properties) getProjectCache(projectKey, APPLICATION_PROPERTIES);
    }

    public static void setDesignerCanvas(String projectKey, DesignerCanvas designerCanvas) {
        putProjectCache(projectKey, CANVAS_PANEL, designerCanvas);
    }

    public static DesignerCanvas getDesignerCanvas(String projectKey) {
        return (DesignerCanvas) getProjectCache(projectKey, CANVAS_PANEL);
    }

    public static void setPropertiesAndCanvasPane(String projectKey, JSplitPane propertiesAndCanvasPane) {
        putProjectCache(projectKey, PROPERTIES_AND_CANVAS_SPLITPANE, propertiesAndCanvasPane);
    }

    public static JSplitPane getPropertiesAndCanvasPane(String projectKey) {
        return (JSplitPane) getProjectCache(projectKey, PROPERTIES_AND_CANVAS_SPLITPANE);
    }

    public static void setPropertiesPanel(String projectKey, ComponentPropertiesPanel componentPropertiesPanel) {
        putProjectCache(projectKey, PROPERTIES_PANEL, componentPropertiesPanel);
    }
    public static ComponentPropertiesPanel getPropertiesPanel(String projectKey) {
        return (ComponentPropertiesPanel) getProjectCache(projectKey, PROPERTIES_PANEL);
    }

    public static void setCanvasTextArea(String projectKey, JTextArea canvasTextArea) {
        putProjectCache(projectKey, CANVAS_TEXT_AREA, canvasTextArea);
    }
    public static JTextArea getCanvasTextArea(String projectKey) {
        return (JTextArea) getProjectCache(projectKey, CANVAS_TEXT_AREA);
    }

    public static void setIkasanModule(String projectKey, Module ikasanModule) {
        putProjectCache(projectKey, IKASAN_MODULE, ikasanModule);
    }
    public static Module getIkasanModule(String projectKey) {
        return (Module) getProjectCache(projectKey, IKASAN_MODULE);
    }
    public static void setPipsiIkasanModel(String projectKey, PIPSIIkasanModel ikasanModule) {
        putProjectCache(projectKey, PIPSI_IKASAN_MODEL, ikasanModule);
    }
    public static PIPSIIkasanModel getPipsiIkasanModel(String projectKey) {
        return (PIPSIIkasanModel) getProjectCache(projectKey, PIPSI_IKASAN_MODEL);
    }
}