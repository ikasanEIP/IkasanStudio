package org.ikasan.studio.model.ikasan.meta;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.io.ComponentDeserialisation;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.ikasan.studio.StudioUtils.getDirectories;

/**
 * This class aggregates all the defined Ikasan components
 */
public class IkasanComponentLibrary {
    private static final String RESOURCE_BASE_BASE_DIR = "studio/";
    private static final String GENERAL_ICONS_DIR = RESOURCE_BASE_BASE_DIR +"icons/";
    private static final String UNKNOWN_ICONS_DIR = GENERAL_ICONS_DIR +"unknown/";
    public static final String STD_IKASAN_PACK = "V3.3.x";  // Short term convenience, long term this must be pak driven

    private static final String SMALL_ICON_NAME = "small.png";
    private static final String NORMAL_ICON_NAME = "normal.png";
    private static final String LARGE_ICON_NAME = "large.png";
    private static final String FLOW = "FLOW";
    private static final String MODULE = "MODULE";
    private static final String EXCEPTION_RESOLVER = "EXCEPTION_RESOLVER";
    private static final Logger LOG = Logger.getInstance("#IkasanComponentLibrary");


    // IkasanVersionPack -> Ikasan Component Name -> Ikasan Component Meta
    protected static Map<String, Map<String, IkasanComponentMeta>> versionedComponenetsLibrary = new HashMap<>(new HashMap<>());
    private static final Set<String> mandatoryComponents = new HashSet<>(Arrays.asList(MODULE, FLOW, EXCEPTION_RESOLVER));

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanVersionPack to search for components
     */
    public static Map<String, IkasanComponentMeta> refreshComponentLibrary(final String ikasanVersionPack) {
        Map<String, IkasanComponentMeta> newIkasanComponentMetaMap
                = new HashMap<>();
        String baseDirectory = RESOURCE_BASE_BASE_DIR + ikasanVersionPack + "/components";
        String[] componentDirectories = null;
        try {
            componentDirectories = getDirectories(baseDirectory);
        } catch (URISyntaxException | IOException e) {
            LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
        }

        assert componentDirectories != null;
        for(String componentName : componentDirectories) {
            final String componentBaseDirectory = baseDirectory+"/"+componentName;
            IkasanMeta ikasanMeta;

            try {
                ikasanMeta = ComponentDeserialisation.deserializeComponent(
                        componentBaseDirectory+"/attributes_en_GB.json"
                );

            } catch (StudioException e) {
                LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                        " there was an error generating the details for component " + componentName +
                        " review the Ikasan version pack, perhaps reinstall or use an alternate version");
                continue;
            }
            IkasanComponentMeta ikasanComponentMeta = (IkasanComponentMeta)ikasanMeta;
            ikasanComponentMeta.setSmallIcon(getImageIcon(componentBaseDirectory + "/" + SMALL_ICON_NAME, UNKNOWN_ICONS_DIR + SMALL_ICON_NAME));
            ikasanComponentMeta.setCanvasIcon(getImageIcon(componentBaseDirectory + "/" + NORMAL_ICON_NAME, UNKNOWN_ICONS_DIR + NORMAL_ICON_NAME));
            newIkasanComponentMetaMap.put(componentName, ikasanComponentMeta);
        }
        if (! newIkasanComponentMetaMap.keySet().containsAll(mandatoryComponents)) {
            LOG.error("The ikasan version pack " + ikasanVersionPack + " did not contain all the mandatory components " +
                    mandatoryComponents + " so will be ignored");
        }

        synchronized (IkasanComponentLibrary.class) {
            versionedComponenetsLibrary.put(ikasanVersionPack, newIkasanComponentMetaMap);
        }
        return newIkasanComponentMetaMap;
    }

    public static IkasanComponentMeta getFLow(final String version) {
        return getIkasanComponent(version, FLOW);
    }
    public static IkasanComponentMeta getModule(final String version) {
        return getIkasanComponent(version, MODULE);
    }
    public static IkasanComponentMeta getExceptionResolver(final String version) {
        return getIkasanComponent(version, EXCEPTION_RESOLVER);
    }
    public static IkasanComponentMeta getOnException(final String version) {
        return getIkasanComponent(version, "OnException");
    }


    /**
     * Attempt to minimize the synchronized lock. We will guarentee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    protected synchronized static Map<String, IkasanComponentMeta> geIkasanComponentMetaMap(final String version) {
        Map<String, IkasanComponentMeta> ikasanComponentMetaMap = versionedComponenetsLibrary.get(version);
        if (ikasanComponentMetaMap == null || ikasanComponentMetaMap.isEmpty()) {
            ikasanComponentMetaMap = refreshComponentLibrary(version);
        }
        return versionedComponenetsLibrary.get(version);
    }

    // Currently, restrict access to the Map
    public static IkasanComponentMeta getIkasanComponent(String version, String key) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMap(version);
        return safeIkasanComponentMetaMap.get(key);
    }

    public static Set<String> getIkasanComponentNames(String version) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMap(version);
        return safeIkasanComponentMetaMap.keySet();
    }
    public static Collection<IkasanComponentMeta>  getIkasanComponentList(String version) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMap(version);
        return safeIkasanComponentMetaMap.values();
    }

    public static int getNumberOfComponents(String version) {
        return getIkasanComponentNames(version).size();
    }

    private static ImageIcon getImageIcon(String iconLocation, String defaultIcon) {
        ImageIcon imageIcon;
        URL iconURL = IkasanComponentLibrary.class.getClassLoader().getResource(iconLocation);
        if (iconURL == null) {
            LOG.warn("Could not create Icon for " + iconLocation + " using default");
            iconURL = IkasanComponentLibrary.class.getClassLoader().getResource(defaultIcon);
        }
        imageIcon = new ImageIcon(iconURL);
        return imageIcon;
    }
}
