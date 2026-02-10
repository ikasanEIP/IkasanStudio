package org.ikasan.studio.ui;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.components.Service;
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
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import javax.swing.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Project-level service that manages UI context and state for a given project.
 *
 * This service replaces the previous singleton pattern with IntelliJ's ProjectService framework,
 * providing:
 * - Better lifecycle management (automatically disposed when project closes)
 * - Thread-safe initialization and access via the service locator
 * - Separation of concerns per project
 * - Simplified testing and dependency injection
 *
 * Usage: {@code UiContext service = project.getService(UiContext.class);}
 *
 * @see com.intellij.openapi.components.Service
 */
@Service(Service.Level.PROJECT)
public final class UiContext {

    public static final String JAVA_FILE_EXTENSION = "java";
    public static final String XML_FILE_EXTENSION = "xml";

    private static final String CANVAS_PANEL = "canvasPanel";
    private static final String PROJECT_REFRESH_TIMESTAMP = "projectRefreshTimeStamp";
    private static final String VIEW_HANDLER_FACTORY = "viewHandlerFactory";
    private static final String OPTIONS = "options";
    private static final String APPLICATION_PROPERTIES = "application_properties";
    private static final String PROPERTIES_PANEL = "propertiesPanel";
    private static final String PROPERTIES_TAB_PANEL = "propertiesTabPanel";
    private static final String PALETTE_PANEL = "palettePanel";
    private static final String RIGHT_TABBED_PANE = "rightTabbedPane";
    public static final String PROPERTIES_TAB_TITLE = "Properties";
    public static final String PALETTE_TAB_TITLE = "Palette";
    public static final int MINIMUM_COMPONENT_X_SPACING = 15;
    public static final int PROPERTIES_TAB_INDEX = 0;
    public static final int PALETTE_TAB_INDEX = 1;
    private static final String DESIGNER_UI = "designerUI";
    private static final String CANVAS_TEXT_AREA = "canvasTextArea";
    private static final String IKASAN_MODULE = "ikasanModule";
    private static final String APPSERVER_PROCESS_HANDLE = "appserverProcessHandle";
    private static final String PIPSI_IKASAN_MODEL = "pipsiIkasanModel";
    private static final String IKASAN_POM_MODEL = "ikasanPomModel";

    private final Project project;
    private final Map<String, Object> cache = new TreeMap<>();

    public UiContext(Project project) {
        this.project = project;
        // Initialize options cache as these are essential
        this.cache.putIfAbsent(OPTIONS, new Options());
    }

    /**
     * The spacing between flowchart components on the diagram.
     * TODO: populate from UI config
     * @return the minimum spacing between flowchart components on the diagram.
     */
    public static int getMinimumComponentXSpacing() {
        return MINIMUM_COMPONENT_X_SPACING;
    }

    private synchronized Object getFromCache(String key) {
        return cache.get(key);
    }

    private synchronized void putInCache(String key, Object value) {
        cache.put(key, value);
    }

    public Options getOptions() {
        return (Options) getFromCache(OPTIONS);
    }

    public IkasanPomModel getIkasanPomModel() {
        IkasanPomModel ikasanPomModel = (IkasanPomModel) getFromCache(IKASAN_POM_MODEL);
        if (ikasanPomModel == null) {
            ikasanPomModel = StudioPsiUtils.pomLoadFromVirtualDisk(project);
            if (ikasanPomModel != null) {
                setIkasanPomModel(ikasanPomModel);
            }
        }
        return ikasanPomModel;
    }

    public void setIkasanPomModel(IkasanPomModel pom) {
        putInCache(IKASAN_POM_MODEL, pom);
    }

    public Long getProjectRefreshTimestamp() {
        Long timestamp = (Long) getFromCache(PROJECT_REFRESH_TIMESTAMP);
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
            setProjectRefreshTimestamp(timestamp);
        }
        return timestamp;
    }

    public void setProjectRefreshTimestamp(Long timestamp) {
        putInCache(PROJECT_REFRESH_TIMESTAMP, timestamp);
    }

    public ViewHandlerCache getViewHandlerFactory() {
        return (ViewHandlerCache) getFromCache(VIEW_HANDLER_FACTORY);
    }

    public void setViewHandlerFactory(ViewHandlerCache viewHandlerFactory) {
        putInCache(VIEW_HANDLER_FACTORY, viewHandlerFactory);
    }

    public Map<String, String> getApplicationProperties() {
        Map<String, String> applicationProperties = (Map<String, String>) getFromCache(APPLICATION_PROPERTIES);
        if (applicationProperties == null) {
            applicationProperties = StudioPsiUtils.getApplicationPropertiesMapFromVirtualDisk(project);
            if (applicationProperties != null) {
                setApplicationProperties(applicationProperties);
            }
        }
        return applicationProperties;
    }

    public void setApplicationProperties(Map<String, String> applicationProperties) {
        putInCache(APPLICATION_PROPERTIES, applicationProperties);
    }

    public void setDesignerCanvas(DesignerCanvas designerCanvas) {
        putInCache(CANVAS_PANEL, designerCanvas);
    }

    public DesignerCanvas getDesignerCanvas() {
        return (DesignerCanvas) getFromCache(CANVAS_PANEL);
    }

    /**
     * Set the selected tab
     * @param tabIndex one of the defined constants PROPERTIES_TAB_INDEX, PALETTE_TAB_INDEX
     */
    public void setRightTabbedPaneFocus(int tabIndex) {
        JTabbedPane rightTabbedPane = getRightTabbedPane();
        if (rightTabbedPane != null) {
            rightTabbedPane.setSelectedIndex(tabIndex);
        }
    }

    public void setRightTabbedPane(JTabbedPane rightTabbedPane) {
        putInCache(RIGHT_TABBED_PANE, rightTabbedPane);
    }

    public JTabbedPane getRightTabbedPane() {
        return (JTabbedPane) getFromCache(RIGHT_TABBED_PANE);
    }

    public void setDesignerUI(DesignerUI designerUI) {
        putInCache(DESIGNER_UI, designerUI);
    }

    public DesignerUI getDesignerUI() {
        return (DesignerUI) getFromCache(DESIGNER_UI);
    }

    public void setPropertiesPanel(ComponentPropertiesPanel componentPropertiesPanel) {
        putInCache(PROPERTIES_PANEL, componentPropertiesPanel);
    }

    public ComponentPropertiesPanel getPropertiesPanel() {
        return (ComponentPropertiesPanel) getFromCache(PROPERTIES_PANEL);
    }

    public void setPropertiesTabPanel(ComponentPropertiesTabPanel componentPropertiesPanel) {
        putInCache(PROPERTIES_TAB_PANEL, componentPropertiesPanel);
    }

    public ComponentPropertiesTabPanel getPropertiesTabPanel() {
        return (ComponentPropertiesTabPanel) getFromCache(PROPERTIES_TAB_PANEL);
    }

    public void setPalettePanel(PaletteTabPanel paletteTabPanel) {
        putInCache(PALETTE_PANEL, paletteTabPanel);
    }

    public PaletteTabPanel getPalettePanel() {
        return (PaletteTabPanel) getFromCache(PALETTE_PANEL);
    }

    public void setCanvasTextArea(JTextArea canvasTextArea) {
        putInCache(CANVAS_TEXT_AREA, canvasTextArea);
    }

    public JTextArea getCanvasTextArea() {
        return (JTextArea) getFromCache(CANVAS_TEXT_AREA);
    }

    public void setIkasanModule(Module ikasanModule) {
        putInCache(IKASAN_MODULE, ikasanModule);
    }

    public Module getIkasanModule() {
        return (Module) getFromCache(IKASAN_MODULE);
    }

    public void setAppserverProcessHandle(ProcessHandler processHandler) {
        putInCache(APPSERVER_PROCESS_HANDLE, processHandler);
    }

    public ProcessHandler getAppserverProcessHandle() {
        return (ProcessHandler) getFromCache(APPSERVER_PROCESS_HANDLE);
    }

    public void setPipsiIkasanModel(PIPSIIkasanModel ikasanModule) {
        putInCache(PIPSI_IKASAN_MODEL, ikasanModule);
    }

    public PIPSIIkasanModel getPipsiIkasanModel() {
        return (PIPSIIkasanModel) getFromCache(PIPSI_IKASAN_MODEL);
    }
}