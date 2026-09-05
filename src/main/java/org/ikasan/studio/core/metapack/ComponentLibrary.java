package org.ikasan.studio.core.metapack;

import org.ikasan.studio.core.metapack.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildRuntimeException;
import org.ikasan.studio.core.metapack.loading.ComponentLibraryLoader;
import org.ikasan.studio.core.metapack.validation.MetaPackValidator;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.metapack.model.ComponentMeta.GENERIC_KEY;

/**
 * This class aggregates all the defined Ikasan components
 */
public final class ComponentLibrary {
    public static final String DEFAULT_IKASAN_PACK = "V3.3.9";  // Short term convenience, long term this must be pak driven

    private static final Logger LOG = LoggerFactory.getLogger(ComponentLibrary.class);
    private static final ComponentLibraryLoader LOADER = new ComponentLibraryLoader();
    private static final Set<String> mandatoryComponents = Set.of(
            ComponentMeta.MODULE_TYPE, ComponentMeta.FLOW_TYPE, ComponentMeta.EXCEPTION_RESOLVER_TYPE);
    /** Immutable packaged-resource index. There is no active/global selected version and no runtime cache mutation. */
    private static final Map<String, LibrarySnapshot> PACKAGED_LIBRARIES = loadPackagedLibraries();

    private record LibrarySnapshot(Map<String, ComponentMeta> byKey,
                                   Map<String, ComponentMeta> byDeserialisationKey,
                                   MetaPackManifest manifest,
                                   String validationError) { }

    public static MetaPackManifest getMetaPackManifest(String version) throws StudioBuildException {
        return snapshot(version).manifest();
    }

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     *
     * @param ikasanMetaDataPackVersion to search for components
     */
    public static void refreshComponentLibrary(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        if (ikasanMetaDataPackVersion == null) {
            LOG.error("STUDIO: ikasanMetaDataPackVersion was set to null which is not allowed");
        }

        snapshot(ikasanMetaDataPackVersion);
    }

    public static boolean versionNotContained(String ikasanMetaDataPackVersion) {
        return !PACKAGED_LIBRARIES.containsKey(ikasanMetaDataPackVersion);
    }

    private static Map<String, LibrarySnapshot> loadPackagedLibraries() {
        Map<String, LibrarySnapshot> libraries = new LinkedHashMap<>();
        String[] directories = LOADER.subdirectories(ComponentLibraryLoader.METAPACK_BASE_DIRECTORY);
        for (String directory : directories) {
            String version = FilenameUtils.getName(directory);
            if (!LOADER.hasManifest(version)) continue;
            LibrarySnapshot snapshot = loadSnapshot(version);
            if (!snapshot.byKey().isEmpty()) libraries.put(version, snapshot);
        }
        return Collections.unmodifiableMap(libraries);
    }

    private static LibrarySnapshot snapshot(String version) throws StudioBuildException {
        LibrarySnapshot packaged = PACKAGED_LIBRARIES.get(version);
        LibrarySnapshot selected = packaged != null ? packaged : loadSnapshot(version);
        if (selected.validationError() != null) {
            throw new StudioBuildException(selected.validationError());
        }
        if (selected.byKey().isEmpty()) {
            throw new StudioBuildException("STUDIO: Attempt to get a meta-pack for key " + version + " resulted in an empty library");
        }
        return selected;
    }

    private static LibrarySnapshot loadSnapshot(String version) {
        Map<String, ComponentMeta> byKey = new LinkedHashMap<>();
        MetaPackManifest manifest;
        try {
            LOADER.validateSources(version);
            loadMetapack(version, byKey);
            manifest = LOADER.loadManifest(version);
            MetaPackValidator.validate(version, manifest, byKey);
        } catch (StudioBuildException e) {
            LOG.error("STUDIO: Meta-pack {} failed compliance validation", version, e);
            return new LibrarySnapshot(Map.of(), Map.of(), null, e.getMessage());
        }
        Map<String, ComponentMeta> byDeserialisationKey = generateDeserialisationKeyedMeta(byKey);
        return new LibrarySnapshot(Collections.unmodifiableMap(byKey),
                Collections.unmodifiableMap(byDeserialisationKey), manifest, null);
    }

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     *
     * @param ikasanMetaDataPackVersion to search for components
     */
    public static void loadMetapack(final String ikasanMetaDataPackVersion,
                                    Map<String, ComponentMeta> returnedIkasanComponentMetaMapByKey) {
        returnedIkasanComponentMetaMapByKey.putAll(LOADER.load(ikasanMetaDataPackVersion));
        if (!returnedIkasanComponentMetaMapByKey.isEmpty()
                && !returnedIkasanComponentMetaMapByKey.keySet().containsAll(mandatoryComponents)) {
            LOG.error("STUDIO: The Ikasan version pack {} did not contain all mandatory components {}",
                    ikasanMetaDataPackVersion, mandatoryComponents);
        }
    }

    /**
     * List all metapack installed.
     */
    public static List<String> getMetapackList() {
        String[] directroies = getSubdirectories(ComponentLibraryLoader.METAPACK_BASE_DIRECTORY);
        if (directroies != null) {

            return Arrays.stream(directroies)
                    .map(FilenameUtils::getName)
                    .filter(LOADER::hasManifest)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static String[] getSubdirectories(String baseDirectory) {
        return LOADER.subdirectories(baseDirectory);
    }

    /**
     * For most internal operations on componentMeta, the name of the component is used e.f. 'FTP Consumer',
     * When we need to deserialize a module.json, the 'names' of the components are not contained, instead we need to use
     * other pieces of data in the module.json to identify components i.e. using a deserialization key.
     * This method creates a map into componentMeta using the deserialization key.
     * @param ikasanComponentMetaMap keyed by component name
     * @return  the provided meta map, keyed by 'deserialization attributes'
     */

    private static Map<String, ComponentMeta> generateDeserialisationKeyedMeta(Map<String, ComponentMeta> ikasanComponentMetaMap) {
        Map<String, ComponentMeta> deserialsiationMetaMap = new HashMap<>();
        if (ikasanComponentMetaMap != null && !ikasanComponentMetaMap.isEmpty()) {
            for (ComponentMeta componentMeta : ikasanComponentMetaMap.values()) {
                String key = getDeserialisationKey(componentMeta);
                if (deserialsiationMetaMap.containsKey(key)) {
                    LOG.error("Studio: Serious: A mapping already exists for key [" + key + "] existing value [" + deserialsiationMetaMap.get(key) + "], new value [" + componentMeta + "]. Correct the metapack. This entry will be ignored");
                } else {
                    deserialsiationMetaMap.putIfAbsent(getDeserialisationKey(componentMeta), componentMeta);
                }
            }
        }
        return deserialsiationMetaMap;
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
        implementingClass = getClassFromSpringString(implementingClass);
        if (implementingClass != null && !implementingClass.isBlank()) {
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

    public static String getClassFromSpringString(String implementingClass) {
        if (implementingClass != null && !implementingClass.isBlank()) {
            if (implementingClass.contains("$")) {
                implementingClass = implementingClass.split("\\$")[0];
            }
        }
        return implementingClass;
    }

    public static ComponentMeta getFLowComponentMeta(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        return getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentMeta.FLOW_TYPE);
    }
    public static ComponentMeta getModuleComponentMetaMandatory(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        ComponentMeta componentMeta = getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentMeta.MODULE_TYPE);
        if (componentMeta == null) {
            throw new StudioBuildRuntimeException("Meta cant be null");
        }
        return componentMeta;
    }
    public static ExceptionResolverMeta getExceptionResolverMetaMandatory(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        ExceptionResolverMeta componentMeta = (ExceptionResolverMeta) getIkasanComponentByKey(ikasanMetaDataPackVersion, ComponentMeta.EXCEPTION_RESOLVER_TYPE);
        if (componentMeta == null) {
            throw new StudioBuildRuntimeException("Meta cant be null");
        }
        return componentMeta;
    }

    /**
     * Attempt to minimize the synchronized lock. We will guarantee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    private static Map<String, ComponentMeta> getIkasanComponentMetaMapByKey(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        return snapshot(ikasanMetaDataPackVersion).byKey();
    }

    /**
     * Attempt to minimize the synchronized lock. We will guarantee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    private static Map<String, ComponentMeta> geIkasanComponentMetaMapByDeserialisationKey(final String ikasanMetaDataPackVersion) throws StudioBuildException {
        return snapshot(ikasanMetaDataPackVersion).byDeserialisationKey();
    }

    public static ComponentMeta getIkasanComponentByKey(String ikasanMetaDataPackVersion, String key) throws StudioBuildException {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = getIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.get(key);
    }

    /**
     * Invoked getIkasanComponentByKey and throws StudioBuildRuntimeException if the meta is not found
     * @param ikasanMetaDataPackVersion to search
     * @param key of the component for which we want meta
     * @return the meta for the supplied component key
     */
    public static ComponentMeta getIkasanComponentByKeyMandatory(String ikasanMetaDataPackVersion, String key) throws StudioBuildException {
        ComponentMeta componentMeta = getIkasanComponentByKey(ikasanMetaDataPackVersion, key);
        if (componentMeta == null) {
            throw new StudioBuildRuntimeException("Meta cant be null");
        }
        return componentMeta;
    }

    /**
     * Some use cases, including deserialization, will need to match the metadata given an implementing class
     * @param ikasanMetaDataPackVersion of the IkasanMetaPack
     * @param implementingClass to be searched for
     * @param componentType fully qualified Ikasan class name for this component type e.g. org.ikasan.spec.component.endpoint.Consumer
     * @param additionalKey to be used if no implementing class is available for this component e.g. Module
     * @return the metadata that matches the name of the implementing class provided, or null
     */
    public static ComponentMeta getIkasanComponentByDeserialisationKey(String ikasanMetaDataPackVersion, String implementingClass, String componentType, String additionalKey) throws StudioBuildException {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = geIkasanComponentMetaMapByDeserialisationKey(ikasanMetaDataPackVersion);
        ComponentMeta componentMeta = safeIkasanComponentMetaMap.get(getDeserialisationKey(implementingClass, componentType, additionalKey));
        // Assume we have a generic / user provided component.
        if (componentMeta == null) {
            componentMeta = safeIkasanComponentMetaMap.get(getDeserialisationKey("", componentType, GENERIC_KEY));
        }
        return componentMeta;
    }



    public static Set<String> getIkasanComponentNames(String ikasanMetaDataPackVersion) throws StudioBuildException {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = getIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.keySet();
    }
//    public static Collection<ComponentMeta>  getIkasanComponentList(String ikasanMetaDataPackVersion) throws StudioBuildException {
//        Map<String, ComponentMeta> safeIkasanComponentMetaMap = getIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
//        return safeIkasanComponentMetaMap.values();
//    }
    public static Collection<ComponentMeta>  getPaletteComponentList(String ikasanMetaDataPackVersion) throws StudioBuildException {
        Map<String, ComponentMeta> safeIkasanComponentMetaMap = getIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
        return safeIkasanComponentMetaMap.values().stream()
                .filter(x -> !x.isEndpoint())
                .collect(Collectors.toList());
    }

    public static Map<String, ComponentMeta> getIkasanComponents(String ikasanMetaDataPackVersion) throws StudioBuildException {
        return getIkasanComponentMetaMapByKey(ikasanMetaDataPackVersion);
    }

    public static int getNumberOfComponents(String ikasanMetaDataPackVersion) throws StudioBuildException {
        return getIkasanComponentNames(ikasanMetaDataPackVersion).size();
    }

    public static FlowElement getEndpointForGivenComponent(String ikasanMetaDataPackVersion, FlowElement targetFlowElement) {
        FlowElement endpointFlowElement = null;
        String endpointComponentName = targetFlowElement.getComponentMeta().getEndpointKey();
        if (endpointComponentName != null) {
            // Get the text to be displayed
            // under the endpoint symbol
            String endpointTextKey = targetFlowElement.getComponentMeta().getEndpointTextKey();
            ComponentProperty propertyValueToDisplay = targetFlowElement.getComponentProperties().get(endpointTextKey);
            String endpointText = "";
            if (propertyValueToDisplay != null) {
                endpointText = propertyValueToDisplay.getValueString();
            }
            ComponentMeta endpointComponentMeta;

            try {
                // Create the endpoint symbol instance
                endpointComponentMeta = ComponentLibrary.getIkasanComponentByKey(ikasanMetaDataPackVersion, endpointComponentName);
                endpointFlowElement = FlowElementFactory.createFlowElement(ikasanMetaDataPackVersion, endpointComponentMeta, targetFlowElement.getContainingFlow(), targetFlowElement.getContainingFlowRoute(), endpointText);
            } catch (StudioBuildException se) {
                LOG.warn("STUDIO: A studio exception was raised, please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
            }
            if (endpointFlowElement == null) {
                LOG.warn("STUDIO: Expected to find endpoint for flow element " + targetFlowElement.getIdentity() + " the key was " + endpointComponentName + " but no endpoint was found");
            }
        }
        return endpointFlowElement;
    }
}
