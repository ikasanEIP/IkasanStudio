package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.io.ComponentDeserialisation;
import org.ikasan.studio.io.PojoDeserialisation;

import javax.swing.*;
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
    public static final String TEST_IKASAN_PACK = "Vtest.x";
    private static final String SMALL_ICON_NAME = "small.png";
    private static final String NORMAL_ICON_NAME = "normal.png";
    private static final String LARGE_ICON_NAME = "large.png";
    private static String FLOW = "FLOW";
    private static final String MODULE = "MODULE";
    private static final String EXCEPTION_RESOLVER = "EXCEPTION_RESOLVER";
    private static final Logger LOG = Logger.getInstance("#IkasanComponentLibrary");


    // IkasanVersionPack -> Ikasan Component Name -> Ikasan Component Meta
    protected static Map<String, Map<String, IkasanComponentMeta>> versionedComponenetsLibrary = new HashMap<>(new HashMap<>());
    private static final Set<String> mandatoryComponents = new HashSet<>(Arrays.asList(MODULE, FLOW, EXCEPTION_RESOLVER));

    public static final IkasanComponentMeta UNKNOWN = IkasanComponentMeta.builder().build();
    /**
     * refresh the component library, by this point the version of Ikasan will have been chosen.
     */
    public static void refreshComponentLibrary() {
        refreshComponentLibrary(STD_IKASAN_PACK);
    }

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanVersionPack to search for components
     */
    public static void refreshComponentLibrary(final String ikasanVersionPack) {
        Map<String, IkasanComponentMeta> newIkasanComponentMetanMap
                = new HashMap<>();
        String baseDirectory = RESOURCE_BASE_BASE_DIR + ikasanVersionPack + "/components";
        String[] componentDirectories = null;
        try {
            componentDirectories = getDirectories(baseDirectory);
        } catch (URISyntaxException e) {
            LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
        }

        assert componentDirectories != null;
        for(String componentName : componentDirectories) {
            final String componentBaseDirectory = baseDirectory+"/"+componentName;
            IkasanComponentMeta ikasanComponentMeta = null;
            IkasanExceptionResolutionMeta ikasanExceptionResolutionMeta = null;
            try {
                if (componentName.equals(EXCEPTION_RESOLVER)) {
                    ikasanExceptionResolutionMeta = PojoDeserialisation.deserializePojo(
                            componentBaseDirectory+"/attributes_en_GB.json",
                            new TypeReference<>() {
                            });
//                   ikasanComponentMeta = PojoDeserialisation.deserializePojo(
//                            componentBaseDirectory+"/attributes_en_GB.json",
//                            new TypeReference<GenericPojo<IkasanExceptionResolutionMeta>>() {});
                    System.out.println("Test " + ikasanExceptionResolutionMeta);

//                } else
//                {
//                    ikasanComponentMeta = PojoDeserialisation.deserializePojo(
//                            componentBaseDirectory+"/attributes_en_GB.json",
//                            new TypeReference<GenericPojo<IkasanExceptionResolutionMeta>>() {});
                }
            } catch (StudioException e) {
                LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                        " there was an error generating the details for component " + componentName +
                        " review the Ikasan version pack, perhaps reinstall or use an alternate version");
                continue;
            }
            ikasanComponentMeta.setSmallIcon(getImageIcon(componentBaseDirectory + "/" + SMALL_ICON_NAME, UNKNOWN_ICONS_DIR + SMALL_ICON_NAME));
            ikasanComponentMeta.setCanvasIcon(getImageIcon(componentBaseDirectory + "/" + NORMAL_ICON_NAME, UNKNOWN_ICONS_DIR + NORMAL_ICON_NAME));
            newIkasanComponentMetanMap.put(componentName, ikasanComponentMeta);
        }
        if (! newIkasanComponentMetanMap.keySet().containsAll(mandatoryComponents)) {
            LOG.error("The ikasan version pack " + ikasanVersionPack + " did not contain all the mandatory components " +
                    mandatoryComponents + " so will be ignored");
        }

        synchronized (IkasanComponentLibrary.class) {
            versionedComponenetsLibrary.put(ikasanVersionPack, newIkasanComponentMetanMap);
        }
    }

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanVersionPack to search for components
     */
    public static void refreshComponentLibraryx(final String ikasanVersionPack) {
        Map<String, IkasanComponentMeta> newIkasanComponentMetanMap
                = new HashMap<>();
        String baseDirectory = RESOURCE_BASE_BASE_DIR + ikasanVersionPack + "/components";
        String[] componentDirectories = null;
        try {
            componentDirectories = getDirectories(baseDirectory);
        } catch (URISyntaxException e) {
            LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
        }

        assert componentDirectories != null;
        for(String componentName : componentDirectories) {
            final String componentBaseDirectory = baseDirectory+"/"+componentName;
            IkasanComponentMetaIfc ikasanComponentMeta = null;
//            IkasanExceptionResolutionMeta ikasanExceptionResolutionMeta = null;
            try {
                if (componentName.equals(EXCEPTION_RESOLVER)) {
                    ikasanComponentMeta = ComponentDeserialisation.deserializeComponent(
                            componentBaseDirectory+"/attributes_en_GB.json"
//                            ,
//                            new TypeReference<GenericPojo<IkasanExceptionResolutionMeta>>() {});
//                   ikasanComponentMeta = PojoDeserialisation.deserializePojo(
//                            componentBaseDirectory+"/attributes_en_GB.json",
//                            new TypeReference<GenericPojo<IkasanExceptionResolutionMeta>>() {});
                    );
                    System.out.println("Test " + ikasanComponentMeta);

//                } else
//                {
//                    ikasanComponentMeta = PojoDeserialisation.deserializePojo(
//                            componentBaseDirectory+"/attributes_en_GB.json",
//                            new TypeReference<GenericPojo<IkasanExceptionResolutionMeta>>() {});
                }
            } catch (StudioException e) {
                LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                        " there was an error generating the details for component " + componentName +
                        " review the Ikasan version pack, perhaps reinstall or use an alternate version");
                continue;
            }
//            ikasanComponentMeta.setSmallIcon(getImageIcon(componentBaseDirectory + "/" + SMALL_ICON_NAME, UNKNOWN_ICONS_DIR + SMALL_ICON_NAME));
//            ikasanComponentMeta.setCanvasIcon(getImageIcon(componentBaseDirectory + "/" + NORMAL_ICON_NAME, UNKNOWN_ICONS_DIR + NORMAL_ICON_NAME));
//            newIkasanComponentMetanMap.put(componentName, ikasanComponentMeta);
        }
        if (! newIkasanComponentMetanMap.keySet().containsAll(mandatoryComponents)) {
            LOG.error("The ikasan version pack " + ikasanVersionPack + " did not contain all the mandatory components " +
                    mandatoryComponents + " so will be ignored");
        }

        synchronized (IkasanComponentLibrary.class) {
            versionedComponenetsLibrary.put(ikasanVersionPack, newIkasanComponentMetanMap);
        }
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
    protected synchronized static Map<String, IkasanComponentMeta> geIkasanComponentMetanMap(final String version) {
        return versionedComponenetsLibrary.get(version);
    }

    // Currently, restrict access to the Map
    public static IkasanComponentMeta getIkasanComponent(String version, String key) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetanMap = geIkasanComponentMetanMap(version);
        return safeIkasanComponentMetanMap.get(key);
    }

    public static Set<String> getIkasanComponentList(String version) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetanMap = geIkasanComponentMetanMap(version);
        return safeIkasanComponentMetanMap.keySet();
    }

    public static int getNumberOfComponents(String version) {
        return getIkasanComponentList(version).size();
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
