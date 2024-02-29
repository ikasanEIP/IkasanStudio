package org.ikasan.studio.model.ikasan.meta;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.io.ComponentIO;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final Logger LOG = Logger.getInstance("#IkasanComponentLibrary");


    // IkasanVersionPack -> Ikasan Component Name -> Ikasan Component Meta
    protected static Map<String, Map<String, ComponentMeta>> libraryByVersionAndKey = new HashMap<>(new HashMap<>());
    protected static Map<String, Map<String, ComponentMeta>> libraryByVersionAndClassOrType = new HashMap<>(new HashMap<>());
    private static final Set<String> mandatoryComponents = new HashSet<>(Arrays.asList(ComponentType.Module.toString(), ComponentType.Flow.toString(), "Exception Resolver"));

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanMetaDataPackVersion to search for components
     */
    public static Map<String, ComponentMeta> refreshComponentLibrary(final String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> returnedIkasanComponentMetaMapByKey;

        if (libraryByVersionAndKey.containsKey(ikasanMetaDataPackVersion)) {
            LOG.warn("The pack for version " + ikasanMetaDataPackVersion + " has already been loaded, the attempt to reload it will be ignored");
            returnedIkasanComponentMetaMapByKey = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        } else {
            returnedIkasanComponentMetaMapByKey = new HashMap<>();
            Map<String, ComponentMeta> ikasanComponentMetaMapByClass = new HashMap<>();

            String baseDirectory = RESOURCE_BASE_BASE_DIR + ikasanMetaDataPackVersion + File.separator + "components";
            String[] componentDirectories = null;
            try {
                componentDirectories = StudioUtils.getDirectories(baseDirectory);
            } catch (URISyntaxException | IOException e) {
                LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
            }

            assert componentDirectories != null;
            for (String componentDirectory : componentDirectories) {
                IkasanMeta ikasanMeta;

                try {
                    ikasanMeta = ComponentIO.deserializeMetaComponent(componentDirectory + "/attributes_en_GB.json");

                } catch (StudioException e) {
                    LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                            " there was an error generating the details for component " + componentDirectory +
                            " review the Ikasan version pack, perhaps reinstall or use an alternate version", e);
                    continue;
                }
                ComponentMeta componentMeta = (ComponentMeta) ikasanMeta;
                final String componentName = componentMeta.getName();
                componentMeta.setSmallIcon(getImageIcon(componentDirectory + File.separator + SMALL_ICON_NAME, UNKNOWN_ICONS_DIR + SMALL_ICON_NAME, "Small " + componentName + " icon"));
                componentMeta.setCanvasIcon(getImageIcon(componentDirectory + File.separator + NORMAL_ICON_NAME, UNKNOWN_ICONS_DIR + NORMAL_ICON_NAME, "Medium " + componentName + " icon"));
                returnedIkasanComponentMetaMapByKey.put(componentName, componentMeta);
                ikasanComponentMetaMapByClass.put(getClassOrType(componentMeta) , componentMeta);
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
     * @param componentMeta that we need the key for
     * @return A string key to uniquely identify this meta
     */
    private static String getClassOrType(ComponentMeta componentMeta) {
        String componentKey = componentMeta.getImplementingClass();

        if (componentKey != null && !componentKey.isBlank()) {
            if (componentKey.contains("$")) {
                // remove any inner class reference
                componentKey = componentKey.split("\\$")[0];
            }
        } else {
            // Rare scenario where there is no implementing class
            componentKey = componentMeta.getComponentType();
        }
        return componentKey;
    }

    public static ComponentMeta getFLow(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentType.Flow.toString());
    }
    public static ComponentMeta getModule(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentType.Module.toString());
    }
    public static ExceptionResolutionMeta getExceptionResolver(final String ikasanMetaDataPackVersion) {
        return (ExceptionResolutionMeta) getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentType.ExceptionResolver.toString());
    }
    public static ComponentMeta getOnException(final String ikasanMetaDataPackVersion) {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, "OnException");
    }


    /**
     * Attempt to minimize the synchronized lock. We will guarantee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    protected synchronized static Map<String, ComponentMeta> geIkasanComponentMetaMapByKey(final String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> ikasanComponentMetaMap = libraryByVersionAndKey.get(ikasanMetaDataPackVersion);
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
    protected synchronized static Map<String, ComponentMeta> geIkasanComponentMetaMapByClassOrType(final String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> ikasanComponentMetaMap = libraryByVersionAndClassOrType.get(ikasanMetaDataPackVersion);
        if (ikasanComponentMetaMap == null || ikasanComponentMetaMap.isEmpty()) {
            refreshComponentLibrary(ikasanMetaDataPackVersion);
        }
        return libraryByVersionAndClassOrType.get(ikasanMetaDataPackVersion);
    }

    public static ComponentMeta getIkasanComponentByKey(String ikasanMetaDataPackVersion, String key) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.get(key);
    }

    /**
     * Invoked getIkasanComponentByKey and throws StudioRuntimeException if the meta is not found
     * @param ikasanMetaDataPackVersion to search
     * @param key of the component for which we want meta
     * @return the meta for the supplied component key
     */
    public static ComponentMeta getIkasanComponentByKeyMandatory(String ikasanMetaDataPackVersion, String key) {
        ComponentMeta componentMeta = getIkasanComponentByKey(ikasanMetaDataPackVersion, key);
        if (componentMeta == null) {
            throw new StudioRuntimeException("Meta cant be null");
        }
        return componentMeta;
    }

    /**
     * Some use cases, including deserialisation, will need to match the metadata given an implementing class
     * @param ikasanMetaDataPackVersion of the IkasanMetaPack
     * @param implementingClass to be searched for
     * @param componentType to be used is no implementing class is available for this component e.g. Module
     * @return the metadata that matches the name of the implmenting class provided, or null
     */
    public static ComponentMeta getIkasanComponentByClassOrType(String ikasanMetaDataPackVersion, String implementingClass, String componentType) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByClassOrType(ikasanMetaDataPackVersion);
        ComponentMeta componentMeta = safeIkasanComponentMetaMap.get(implementingClass);
        if (componentMeta == null) {
            LOG.error("Could not find mtea for implementingClass " + implementingClass);
//            LOG.warn("could not find omponent Meta for implementing class " + implementingClass + " now trying componentType " + componentType);
//            componentMeta = safeIkasanComponentMetaMap.get(componentType);
        }
        return componentMeta;
    }

    public static Set<String> getIkasanComponentNames(String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.keySet();
    }
    public static Collection<ComponentMeta>  getIkasanComponentList(String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.values();
    }
    public static Collection<ComponentMeta>  getPaletteComponentList(String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.values().stream().filter(x -> !x.isEndpoint).collect(Collectors.toList());
    }

    public static Map<String, ComponentMeta> getIkasanComponents(String ikasanMetaDataPackVersion) {
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
