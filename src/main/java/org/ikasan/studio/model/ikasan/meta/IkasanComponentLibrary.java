package org.ikasan.studio.model.ikasan.meta;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.compress.utils.FileNameUtils;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.io.ComponentIO;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

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
    public static final String FLOW = "FLOW";
    public static final String FLOW_NAME = "Flow";
    public static final String MODULE = "MODULE";
    public static final String MODULE_NAME = "Module";
    public static final String EXCEPTION_RESOLVER = "EXCEPTION_RESOLVER";
    public static final String EXCEPTION_RESOLVER_NAME = "Exception Resolver";
    private static final Logger LOG = Logger.getInstance("#IkasanComponentLibrary");


    // IkasanVersionPack -> Ikasan Component Name -> Ikasan Component Meta
    protected static Map<String, Map<String, IkasanComponentMeta>> libraryByVersionAndKey = new HashMap<>(new HashMap<>());
    protected static Map<String, Map<String, IkasanComponentMeta>> libraryByVersionAndClassOrType = new HashMap<>(new HashMap<>());
    private static final Set<String> mandatoryComponents = new HashSet<>(Arrays.asList(MODULE, FLOW, EXCEPTION_RESOLVER));

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanMetaDataPackVersion to search for components
     */
    public static Map<String, IkasanComponentMeta> refreshComponentLibrary(final String ikasanMetaDataPackVersion) {
        Map<String, IkasanComponentMeta> returnedIkasanComponentMetaMapByKey;

        if (libraryByVersionAndKey.containsKey(ikasanMetaDataPackVersion)) {
            LOG.warn("The pack for version " + ikasanMetaDataPackVersion + " has already been loaded, the attempt to reload it will be ignored");
            returnedIkasanComponentMetaMapByKey = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        } else {
            returnedIkasanComponentMetaMapByKey = new HashMap<>();
            Map<String, IkasanComponentMeta> ikasanComponentMetaMapByClass = new HashMap<>();

            String baseDirectory = RESOURCE_BASE_BASE_DIR + ikasanMetaDataPackVersion + File.separator + "components";
            String[] componentDirectories = null;
            try {
                componentDirectories = StudioUtils.getDirectories(baseDirectory);
            } catch (URISyntaxException | IOException e) {
                LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
            }

            assert componentDirectories != null;
            for (String componentDirectory : componentDirectories) {
                final String componentName = FileNameUtils.getBaseName(componentDirectory);
                IkasanMeta ikasanMeta;

                try {
                    ikasanMeta = ComponentIO.deserializeMetaComponent(componentDirectory + "/attributes_en_GB.json");

                } catch (StudioException e) {
                    LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                            " there was an error generating the details for component " + componentDirectory +
                            " review the Ikasan version pack, perhaps reinstall or use an alternate version", e);
                    continue;
                }
                IkasanComponentMeta ikasanComponentMeta = (IkasanComponentMeta) ikasanMeta;
                ikasanComponentMeta.setSmallIcon(getImageIcon(componentDirectory + File.separator + SMALL_ICON_NAME, UNKNOWN_ICONS_DIR + SMALL_ICON_NAME, "Small " + componentName + " icon"));
                ikasanComponentMeta.setCanvasIcon(getImageIcon(componentDirectory + File.separator + NORMAL_ICON_NAME, UNKNOWN_ICONS_DIR + NORMAL_ICON_NAME, "Medium " + componentName + " icon"));
                returnedIkasanComponentMetaMapByKey.put(componentName, ikasanComponentMeta);
                ikasanComponentMetaMapByClass.put(getClassOrType(ikasanComponentMeta) , ikasanComponentMeta);
            }
            if (!returnedIkasanComponentMetaMapByKey.keySet().containsAll(mandatoryComponents)) {
                LOG.error("The ikasan version pack " + ikasanMetaDataPackVersion + " contained these components [" +
                        returnedIkasanComponentMetaMapByKey.keySet() + "] but did not contain all the mandatory components " +
                        mandatoryComponents + " so will be ignored");
            }
            if(ikasanComponentMetaMapByClass.size() != returnedIkasanComponentMetaMapByKey.size()) {
                LOG.warn("WARNING: ikasanComponentMetaMapByClass & returnedIkasanComponentMetaMapByKey are different sizes. " +
                        "Keys for returnedIkasanComponentMetaMapByKey [" + returnedIkasanComponentMetaMapByKey.keySet() + "]" +
                        "Keys for ikasanComponentMetaMapByClass [" + ikasanComponentMetaMapByClass.keySet() + "]");
            }

            // @TODO consider synchronizedMap
            synchronized (IkasanComponentLibrary.class) {
                libraryByVersionAndKey.put(ikasanMetaDataPackVersion, returnedIkasanComponentMetaMapByKey);
                libraryByVersionAndClassOrType.put(ikasanMetaDataPackVersion, ikasanComponentMetaMapByClass);
            }
        }
        return returnedIkasanComponentMetaMapByKey;
    }

    /**
     * Attempt to extract a key, give preference to implementing class, if undefined, fall back to componentType
     * @param ikasanComponentMeta that we need the key for
     * @return A string key to uniquely identify this meta
     */
    private static String getClassOrType(IkasanComponentMeta ikasanComponentMeta) {
        String componentKey = ikasanComponentMeta.getImplementingClass();

        if (componentKey != null && !componentKey.isBlank()) {
            if (componentKey.contains("$")) {
                // remove any inner class reference
                componentKey = componentKey.split("\\$")[0];
            }
        } else {
            // Rare scenario where there is no implementing class
            componentKey = ikasanComponentMeta.getComponentType();
        }
        return componentKey;
    }

    public static IkasanComponentMeta getFLow(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, FLOW);
    }
    public static IkasanComponentMeta getModule(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, MODULE);
    }
    public static IkasanExceptionResolutionMeta getExceptionResolver(final String ikasanMetaDataPackVersion) {
        return (IkasanExceptionResolutionMeta) getIkasanComponentByKey(ikasanMetaDataPackVersion, EXCEPTION_RESOLVER);
    }
    public static IkasanComponentMeta getOnException(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, "OnException");
    }


    /**
     * Attempt to minimize the synchronized lock. We will guarantee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    protected synchronized static Map<String, IkasanComponentMeta> geIkasanComponentMetaMapByKey(final String ikasanMetaDataPackVersion) {
        Map<String, IkasanComponentMeta> ikasanComponentMetaMap = libraryByVersionAndKey.get(ikasanMetaDataPackVersion);
        if (ikasanComponentMetaMap == null || ikasanComponentMetaMap.isEmpty()) {
            refreshComponentLibrary(ikasanMetaDataPackVersion);
        }
        return libraryByVersionAndKey.get(ikasanMetaDataPackVersion);
    }
    /**
     * Attempt to minimize the synchronized lock. We will guarantee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    protected synchronized static Map<String, IkasanComponentMeta> geIkasanComponentMetaMapByClassOrType(final String ikasanMetaDataPackVersion) {
        Map<String, IkasanComponentMeta> ikasanComponentMetaMap = libraryByVersionAndClassOrType.get(ikasanMetaDataPackVersion);
        if (ikasanComponentMetaMap == null || ikasanComponentMetaMap.isEmpty()) {
            refreshComponentLibrary(ikasanMetaDataPackVersion);
        }
        return libraryByVersionAndClassOrType.get(ikasanMetaDataPackVersion);
    }

    public static IkasanComponentMeta getIkasanComponentByKey(String ikasanMetaDataPackVersion, String key) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.get(key);
    }

    /**
     * Some use cases, including deserialisation, will need to match the metadata given an implementing class
     * @param ikasanMetaDataPackVersion of the IkasanMetaPack
     * @param implementingClass to be searched for
     * @param componentType to be used is no implementing class is available for this component e.g. Module
     * @return the metadata that matches the name of the implmenting class provided, or null
     */
    public static IkasanComponentMeta getIkasanComponentByClassOrType(String ikasanMetaDataPackVersion, String implementingClass, String componentType) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByClassOrType(ikasanMetaDataPackVersion);
        IkasanComponentMeta ikasanComponentMeta = safeIkasanComponentMetaMap.get(implementingClass);
        if (ikasanComponentMeta == null) {
            LOG.error("Could not find mtea for implementingClass " + implementingClass);
//            LOG.warn("could not find omponent Meta for implementing class " + implementingClass + " now trying componentType " + componentType);
//            ikasanComponentMeta = safeIkasanComponentMetaMap.get(componentType);
        }
        return ikasanComponentMeta;
    }

    public static Set<String> getIkasanComponentNames(String ikasanMetaDataPackVersion) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.keySet();
    }
    public static Collection<IkasanComponentMeta>  getIkasanComponentList(String ikasanMetaDataPackVersion) {
        Map<String, IkasanComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.values();
    }

    public static Map<String, IkasanComponentMeta> getIkasanComponents(String ikasanMetaDataPackVersion) {
        return geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
    }

    public static int getNumberOfComponents(String ikasanMetaDataPackVersion) {
        return getIkasanComponentNames(ikasanMetaDataPackVersion).size();
    }

    private static ImageIcon getImageIcon(String iconLocation, String defaultIcon, String description) {
        ImageIcon imageIcon;
        URL iconURL = IkasanComponentLibrary.class.getClassLoader().getResource(iconLocation);
        if (iconURL == null) {
            LOG.warn("Could not create Icon for " + iconLocation + " using default");
            iconURL = IkasanComponentLibrary.class.getClassLoader().getResource(defaultIcon);
        }
        imageIcon = new ImageIcon(iconURL, description);
        return imageIcon;
    }
}
