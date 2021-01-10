package org.ikasan.studio;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.component.DesignerCanvas;
import org.ikasan.studio.ui.component.PropertiesPanel;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The Context allows all the independant parts of the UI to collaborate with each other.
 *
 * Anything can be placed in the Context, some things are so important that they will have their own getters
 * just for convenience.
 */
public class Context {
    private static final String CANVAS_PANEL = "canvasPanel";
    private static final String PROJECT = "project";
    private static final String PROPERTIES_PANEL = "propertiesPanel";
    private static final String PROPERTIES_AND_CANVAS_SPLITPANE = "propertiesAndCanvasSplitPane";
    private static final String CANVAS_TEXT_AREA = "canvasTextArea";
    private static final String IKASAN_MODULE = "ikasanModule";
    private static final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";

    private static Map<String, Map<String, Object>> projectCache = new HashMap<>();

    public static Map<String, Map<String, Object>> getProjectCache() {
        return projectCache;
    }

    private static synchronized void putProjectCache(String projectKey, String key, Object value) {
        Map cache = projectCache.get(projectKey);
        if (cache == null) {
            cache = new HashMap<String, Object>();
            projectCache.put(projectKey, cache);
        }
        cache.put(key, value);
    }

    private static synchronized Object getProjectCache(String projectKey, String key) {
        Map cache = projectCache.get(projectKey);
        if (cache != null) {
            return cache.get(key);
        }
        return null;
    }

    public static Project getProject(String projectKey) {
        return (Project)getProjectCache(projectKey, PROJECT);
    }

    public static void setProject(String projectKey, Project project) {
        putProjectCache(projectKey, PROJECT, project);
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

    public static void setPropertiesPanel(String projectKey, PropertiesPanel propertiesPanel) {
        putProjectCache(projectKey, PROPERTIES_PANEL, propertiesPanel);
    }
    public static PropertiesPanel getPropertiesPanel(String projectKey) {
        return (PropertiesPanel) getProjectCache(projectKey, PROPERTIES_PANEL);
    }

    public static void setCanvasTextArea(String projectKey, JTextArea canvasTextArea) {
        putProjectCache(projectKey, CANVAS_TEXT_AREA, canvasTextArea);
    }
    public static JTextArea getCanvasTextArea(String projectKey) {
        return (JTextArea) getProjectCache(projectKey, CANVAS_TEXT_AREA);
    }

    public static void setIkasanModule(String projectKey, IkasanModule ikasanModule) {
        putProjectCache(projectKey, IKASAN_MODULE, ikasanModule);
    }
    public static IkasanModule getIkasanModule(String projectKey) {
        return (IkasanModule) getProjectCache(projectKey, IKASAN_MODULE);
    }
    public static void setPipsiIkasanModel(String projectKey, PIPSIIkasanModel ikasanModule) {
        putProjectCache(projectKey, PIPSI_IKASAN_MODEL, ikasanModule);
    }
    public static PIPSIIkasanModel getPipsiIkasanModel(String projectKey) {
        return (PIPSIIkasanModel) getProjectCache(projectKey, PIPSI_IKASAN_MODEL);
    }
}