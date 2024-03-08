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
    protected static Map<String, Map<String, ComponentMeta>> libraryByVersionAndDeserialisationKey = new HashMap<>(new HashMap<>());
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
                ikasanComponentMetaMapByClass.put(getDeserialisationKey(componentMeta) , componentMeta);
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
                libraryByVersionAndDeserialisationKey.put(ikasanMetaDataPackVersion, ikasanComponentMetaMapByClass);
            }
        }
        return returnedIkasanComponentMetaMapByKey;
    }


    /**
     * When reading Module flows from JSON we have only implementingClass and componentType in the standard Ikasan
     * 'module.json'. This is insufficient to uniquely identify some components e.g. Local File Consumer so an
     * additional key is also provided (usually the component name). Most of the time the additional key is not provided
     * @param implementingClass of the component
     * @param componentType of the component
     * @param additionalKey of the component (usually the component name, only needed if the above 2 are not unique)
     * @return A unique key used to access
     */
    public static String getDeserialisationKey(String implementingClass, String componentType, String additionalKey) {
        StringBuilder metaDataDeserialisationKey = new StringBuilder();
        if (implementingClass != null && !implementingClass.isBlank()) {
            if (implementingClass.contains("$")) {
                // remove any inner class reference
                implementingClass = implementingClass.split("\\$")[0];
            }
            metaDataDeserialisationKey.append(implementingClass).append("-");
        }
        if (componentType != null && !componentType.isBlank()) {
            metaDataDeserialisationKey.append(componentType).append("-");
        }
        if (additionalKey != null && !additionalKey.isBlank()) {
            metaDataDeserialisationKey.append(additionalKey);
        }
        return metaDataDeserialisationKey.toString();
    }
    public static String getDeserialisationKey(ComponentMeta componentMeta) {
        return getDeserialisationKey(componentMeta.getImplementingClass(), componentMeta.getComponentType(), componentMeta.getAdditionalKey());
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
    protected synchronized static Map<String, ComponentMeta> geIkasanComponentMetaMapByDeserialisationKey(final String ikasanMetaDataPackVersion) {
        Map<String, ComponentMeta> ikasanComponentMetaMap = libraryByVersionAndDeserialisationKey.get(ikasanMetaDataPackVersion);
        if (ikasanComponentMetaMap == null || ikasanComponentMetaMap.isEmpty()) {
            refreshComponentLibrary(ikasanMetaDataPackVersion);
            ikasanComponentMetaMap = libraryByVersionAndDeserialisationKey.get(ikasanMetaDataPackVersion);
        }
        return ikasanComponentMetaMap;
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
     * @param additionalKey to be used is no implementing class is available for this component e.g. Module
     * @return the metadata that matches the name of the implmenting class provided, or null
     */
    public static ComponentMeta getIkasanComponentByDeserialisationKey(String ikasanMetaDataPackVersion, String implementingClass, String componentType, String additionalKey) {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByDeserialisationKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.get(getDeserialisationKey(implementingClass, componentType, additionalKey));
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
        return safeIkasanComponentMetaMap.values().stream().filter(x -> !x.isEndpoint()).collect(Collectors.toList());
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
