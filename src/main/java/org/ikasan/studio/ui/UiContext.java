package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Options;
import org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
/**
 * The UiContext allows all the independent parts of the UI to collaborate with each other.
 * Anything can be placed in the UiContext, some things are so important that they will have their own getters
 * just for convenience.
 */
public enum UiContext {
    INSTANCE;
    public static final String JAVA_FILE_EXTENSION = "java";
    public static final String XML_FILE_EXTENSION = "xml";

    private static final String CANVAS_PANEL = "canvasPanel";
    private static final String PROJECT = "project";
    private static final String VIEW_HANDLER_FACTORY = "viewHandlerFactory";
    private static final String OPTIONS = "options";
    private static final String APPLICATION_PROPERTIES = "application_properties";
    private static final String PROPERTIES_PANEL = "propertiesPanel";
    private static final String PROPERTIES_TAB_PANEL = "propertiesTabPanel";
    private static final String PALETTE_PANEL = "palettePanel";
    private static final String RIGHT_TABBED_PANE = "rightTabbedPane";
    public static final String PROPERTIES_TAB_TITLE = "Properties";
    public static final String PALETTE_TAB_TITLE = "Palette";
    public static final int PROPERTIES_TAB_INDEX = 0;
    public static final int PALETTE_TAB_INDEX = 1;
    private static final String DESIGNER_UI = "designerUI";
    private static final String CANVAS_TEXT_AREA = "canvasTextArea";
    private static final String IKASAN_MODULE = "ikasanModule";
    private static final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";
    private static final String IKASAN_POM_MODEL = "ikasanPomModel";



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

    public static IkasanPomModel getIkasanPomModel(String projectKey) {
        IkasanPomModel ikasanPomModel = (IkasanPomModel)getProjectCache(projectKey, IKASAN_POM_MODEL);
        if (ikasanPomModel == null) {
            Project project = getProject(projectKey);
            ikasanPomModel = StudioPsiUtils.pomLoadFromVirtualDisk(project, project.getName());
        }
        return ikasanPomModel;
    }

    public static void setIkasanPomModel(String projectKey, IkasanPomModel pom) {
        putProjectCache(projectKey, IKASAN_POM_MODEL, pom);
    }

    public static Project getProject(String projectKey) {
        return (Project)getProjectCache(projectKey, PROJECT);
    }

    public static void setProject(String projectKey, Project project) {
        putProjectCache(projectKey, PROJECT, project);
    }

    public static ViewHandlerFactoryIntellij getViewHandlerFactory(String projectKey) {
        return (ViewHandlerFactoryIntellij)getProjectCache(projectKey, VIEW_HANDLER_FACTORY);
    }
    public static Map<String, String> getApplicationProperties(String projectKey) {
        Map<String, String> applicationProperties = (Map)getProjectCache(projectKey, APPLICATION_PROPERTIES);
        if (applicationProperties == null) {
            applicationProperties = StudioPsiUtils.getApplicationPropertiesMapFromVirtualDisk(getProject(projectKey));
            if (applicationProperties != null) {
                setApplicationProperties(projectKey, applicationProperties);
            }
        }
        return applicationProperties;
    }

    public static void setApplicationProperties(String projectKey, Map<String, String> applicationProperties) {
        putProjectCache(projectKey, APPLICATION_PROPERTIES, applicationProperties);
    }


    public static void setViewHandlerFactory(String projectKey, ViewHandlerFactoryIntellij viewHandlerFactory) {
        putProjectCache(projectKey, VIEW_HANDLER_FACTORY, viewHandlerFactory);
    }

    public static void setDesignerCanvas(String projectKey, DesignerCanvas designerCanvas) {
        putProjectCache(projectKey, CANVAS_PANEL, designerCanvas);
    }

    public static DesignerCanvas getDesignerCanvas(String projectKey) {
        return (DesignerCanvas) getProjectCache(projectKey, CANVAS_PANEL);
    }

    /**
     * Set the selected tab
     * @param projectKey to namespace the project
     * @param tabIndex one of the defined constants PROPERTIES_TAB_INDEX, PALETTE_TAB_INDEX
     */
    public static void setRightTabbedPaneFocus(String projectKey, int tabIndex) {
        JTabbedPane rightTabbedPane = getRightTabbedPane(projectKey);
        if (rightTabbedPane != null) {
            rightTabbedPane.setSelectedIndex(tabIndex);
        }
    }
    public static void setRightTabbedPane(String projectKey, JTabbedPane rightTabbedPane) {
        putProjectCache(projectKey, RIGHT_TABBED_PANE, rightTabbedPane);
    }
    public static JTabbedPane getRightTabbedPane(String projectKey) {
        return (JTabbedPane) getProjectCache(projectKey, RIGHT_TABBED_PANE);
    }

    public static void setDesignerUI(String projectKey, DesignerUI designerUI) {
        putProjectCache(projectKey, DESIGNER_UI, designerUI);
    }
    public static DesignerUI getDesignerUI(String projectKey) {
        return (DesignerUI) getProjectCache(projectKey, DESIGNER_UI);
    }

    public static void setPropertiesPanel(String projectKey, ComponentPropertiesPanel componentPropertiesPanel) {
        putProjectCache(projectKey, PROPERTIES_PANEL, componentPropertiesPanel);
    }
    public static ComponentPropertiesPanel getPropertiesPanel(String projectKey) {
        return (ComponentPropertiesPanel) getProjectCache(projectKey, PROPERTIES_PANEL);
    }
    public static void setPropertiesTabPanel(String projectKey, ComponentPropertiesTabPanel componentPropertiesPanel) {
        putProjectCache(projectKey, PROPERTIES_TAB_PANEL, componentPropertiesPanel);
    }
    public static ComponentPropertiesTabPanel getPropertiesTabPanel(String projectKey) {
        return (ComponentPropertiesTabPanel) getProjectCache(projectKey, PROPERTIES_TAB_PANEL);
    }
    public static void setPalettePanel(String projectKey, PaletteTabPanel paletteTabPanel) {
        putProjectCache(projectKey, PALETTE_PANEL, paletteTabPanel);
    }
    public static PaletteTabPanel getPalettePanel(String projectKey) {
        return (PaletteTabPanel) getProjectCache(projectKey, PALETTE_PANEL);
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