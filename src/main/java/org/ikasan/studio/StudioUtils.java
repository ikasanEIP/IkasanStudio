package org.ikasan.studio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.IkasanElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.Element;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Studio Utils
 */
public class StudioUtils {
    private static final Logger LOG = Logger.getInstance("#StudioUtils");

    // Enforce as a untility only class
    private StudioUtils () {}

    /**
     * Given a string delimited by tokens e.g. this.is.my.class.bob then get the last string, bob in this case
     * @param delimeter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input string to analyse
     * @return The last stoken of the string or an empty sp
     */
    public static String getLastToken(String delimeter, String input) {
        String returnString = "";
        if (input != null && delimeter != null) {
            String [] tokens = input.split(delimeter, -1);
            if (tokens.length > 0) {
                returnString = tokens[tokens.length-1];
            }
        }
        return returnString;
    }

    /**
     * Given a string delimited by tokens e.g. this.is.my.class.bob then get all but the last string, this.is.my.class in this case
     * @param delimeter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input string to analyse
     * @return All but the last stoken of the string or an empty sp
     */
    public static String getAllButLastToken(String delimeter, String input) {
        StringBuilder returnString = new StringBuilder();
        if (input != null && delimeter != null) {
            String [] tokens = input.split(delimeter, -1);
            if (tokens.length > 1) {
                returnString = returnString.append(tokens[0]);
                for(int ii = 1; ii < tokens.length-1 ; ii++) {
                    returnString.append(".").append(tokens[ii]);
                }
            }
        }
        return returnString.toString();
    }

    /**
     * Convert the supplied string so that it confirms to the naming rules for java classnames
     * @param input string to be converted
     * @return the input string in the form of a java classname
     */
    public static String toJavaClassName(final String input) {
        String identifer = toJavaIdentifier(input);
        if (!identifer.isEmpty()) {
            char first = Character.toUpperCase(identifer.charAt(0));
            identifer = first + identifer.substring(1);
        }
        return identifer;
    }

    /**
     * Convert the supplied string so that it confirms to the naming rules for java package names i.e.
     *    no spaces
     *    if there is a leading digit, prepend with _
     * @param input string to be converted
     * @return the input string in the form of a java package
     */
    public static String toJavaPackageName(String input) {
        if (input != null && !input.isEmpty()) {
            if (Character.isDigit(input.charAt(0))) {
                input = "_" + input;
            }
            return  input.replaceAll("[^a-zA-Z0-9_]+", "").toLowerCase();
        } else {
            return input;
        }
    }

    /**
     * Convert the supplied string to url style string i.e.
     *   space and undersocre replaced by -
     *   all lower case
     * @param input string to be converted
     * @return the input string in kebab case
     */
    public static String toUrlString(final String input) {
        if (input != null && !input.isEmpty()) {
            return  input
                    .replaceAll("  +", " ")
                    .replaceAll("[ -]+", "-").toLowerCase();
        } else {
            return input;
        }
    }

    /**
     * Pretty much what org.apache.commons.text.CaseUtils does i.e. produce camelCase, but we are limited by what libs Intellij
     * pull into the plugin componentDependencies.
     * @param input a string potentially with spaces
     * @return a string that could be used as a java identifer
     */
    public static String toJavaIdentifier(final String input) {
        if (input != null && !input.isEmpty()) {
            int inputStringLength = input.length();
            char[] inputString = input.toCharArray();
            int outputStringLength = 0;

            boolean toUpper = false;
            for (int inputStringIndex = 0; inputStringIndex < inputStringLength; inputStringIndex++)
            {
                if (inputString[inputStringIndex] == ' ' || inputString[inputStringIndex] == '.') {
                    toUpper = true;
                }
                else {
                    char current;
                    if (outputStringLength == 0) {
                        current = Character.toLowerCase(inputString[inputStringIndex]);
                        if (! (Character.isJavaIdentifierStart(current))) {
                            continue;
                        }
                    } else {
                        if (toUpper) {
                            current = Character.toUpperCase(inputString[inputStringIndex]);
                            if (!Character.isJavaIdentifierPart(current)) {
                                continue;
                            }
                            toUpper = false;
                        } else {
                            current = inputString[inputStringIndex];
                            if (!Character.isJavaIdentifierPart(current)) {
                                continue;
                            }
                        }
                    }
                    inputString[outputStringLength++] = current;
                }
            }
            return String.valueOf(inputString, 0, outputStringLength);
        } else {
            return "";
        }
    }
    private static final int PROPERTY_NAME_INDEX = 0;
    private static final int PARAM_GROUP_NUMBER = 1;
    private static final int CAUSES_USER_CODE_REGENRATION_INDEX = 2;
    private static final int MANDATORY_INDEX = 3;
    private static final int USER_IMPLEMENTED_CLASS_INDEX = 4;
    private static final int SETTER_PROPERTY_INDEX = 5;
    private static final int USER_DEFINED_RESOURCE_INDEX = 6;
    private static final int PROPERTY_CONFIG_LABEL_INDEX = 7;
    private static final int PROPERTY_DATA_TYPE_INDEX = 8;
    private static final int USAGE_DATA_TYPE_INDEX = 9;
    private static final int VALIDATION_INDEX = 10;
    private static final int VALIDATION_MESSAGE_INDEX = 11;
    private static final int DEFAULT_VALUE_INDEX = 12;
    private static final int HELP_INDEX = 13;
    private static final int NUMBER_OF_CONFIGS = 14;
    public static final String COMPONENT_DEFINTIONS_DIR = "/studio/componentDefinitions/";




    public static String[] getDirectories(String dir) throws URISyntaxException {
        File file = new File(Objects.requireNonNull(StudioUtils.class.getClassLoader().getResource(dir)).toURI());
        return file.list((current, name) -> new File(current, name).isDirectory());
    }

    public static Map<String, IkasanComponentPropertyMeta> readIkasanComponentProperties(String propertiesFile) {
//        Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> componentProperties = new TreeMap<>();
//        Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> componentProperties = new LinkedHashMap<>();
        Map<String, IkasanComponentPropertyMeta> componentProperties = new LinkedHashMap<>();
//        componentProperties.put(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT);

        String propertiesFileName = COMPONENT_DEFINTIONS_DIR + propertiesFile + "_en_GB.csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);
        Set<String> propertyConfigLabels = new HashSet<>();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }
                    String[] split = line.split("\\|");
                    if (split.length != NUMBER_OF_CONFIGS) {
                        LOG.warn("An incorrect config has been supplied, incorrect number of configs (should be " + NUMBER_OF_CONFIGS +
                                ", was " + split.length + "), please remove from properties file [" + propertiesFile + "] or fix, the line was [" + line+ "]");
                        continue;
                    }

                    int paramGroupNumber = 1;
                    try {
                        paramGroupNumber = Integer.parseInt(split[PARAM_GROUP_NUMBER]);

                    } catch (NumberFormatException nfe) {
                        LOG.warn("Error trying to parse paramGroupNumber=" + split[PARAM_GROUP_NUMBER] + " config line " + line + "will be ignored, please remove from " + propertiesFile + " or correct it ");
                        continue;
                    }
                    boolean causesUserCodeRegeneration = Boolean.parseBoolean(split[CAUSES_USER_CODE_REGENRATION_INDEX]);
                    String propertyName = split[PROPERTY_NAME_INDEX];
                    String parentPropertyName = null;
                    // Check to see if this property has sub properties
                    if (propertyName.contains(".")) {
                        String[] parentChildPropertyNames = propertyName.split("\\.");
                        parentPropertyName = parentChildPropertyNames[0];
                        propertyName = parentChildPropertyNames[1];
                    } /// need to check size here and throw log error

//                    IkasanComponentPropertyMetaKey newKey = new IkasanComponentPropertyMetaKey(propertyName, paramGroupNumber);
//                    IkasanComponentPropertyMetaKey newKey = new IkasanComponentPropertyMetaKey(propertyName);
                    String newKey = propertyName;
                    if (componentProperties.containsKey(newKey)) {
                        LOG.warn("A property of this key [" + newKey + "] already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                        continue;
                    }

                    final String propertyConfigLabel = split[PROPERTY_CONFIG_LABEL_INDEX];
                    if (propertyConfigLabel != null && !propertyConfigLabel.isEmpty()) {
                        if (propertyConfigLabels.contains(propertyConfigLabel)) {
                            LOG.warn("A property of this propertyConfigLabel already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                            continue;
                        } else {
                            propertyConfigLabels.add(propertyConfigLabel);
                        }
                    }

                    boolean isMandatory = Boolean.parseBoolean(split[MANDATORY_INDEX]);
                    boolean isUserImplementedClass = Boolean.parseBoolean(split[USER_IMPLEMENTED_CLASS_INDEX]);
                    boolean isSetterProperty = Boolean.parseBoolean(split[SETTER_PROPERTY_INDEX]);
                    boolean isUserDefinedResource = Boolean.parseBoolean(split[USER_DEFINED_RESOURCE_INDEX]);

                    String usageDataType = split[USAGE_DATA_TYPE_INDEX];
                    String validation = split[VALIDATION_INDEX];
                    String validationMessage = split[VALIDATION_MESSAGE_INDEX];

                    // Data type
                    Class propertyDataType = null;
                    if (split[PROPERTY_DATA_TYPE_INDEX] != null && !split[PROPERTY_DATA_TYPE_INDEX].isEmpty()) {
                        try {
                            propertyDataType = Class.forName(split[PROPERTY_DATA_TYPE_INDEX]);
                        } catch (ClassNotFoundException ex) {
                            LOG.warn("An error has occurred while determining the class for " + line + " please remove from " + propertiesFile + " or correct it ", ex);
                            propertyDataType = String.class;  // dont crash the IDE
                        }
                    }

                    //  default value
                    Object defaultValue = getDefaultValue(split, propertyDataType, line,  propertiesFile);

                    // XXXXX try JSON deserialise here

                    IkasanComponentPropertyMeta ikasanComponentPropertyMeta = IkasanComponentPropertyMeta.builder().build();
//                    IkasanComponentPropertyMeta ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(
//                            paramGroupNumber, causesUserCodeRegeneration, isMandatory, isUserImplementedClass, isSetterProperty, isUserDefinedResource, propertyName, propertyConfigLabel,
//                            propertyDataType, usageDataType, validation, validationMessage, defaultValue, split[HELP_INDEX]);
//                    if (parentPropertyName != null) {
                        // Parent child relationship
//                        IkasanComponentPropertyMetaKey parentKey = new IkasanComponentPropertyMetaKey(parentPropertyName, paramGroupNumber);
//                        IkasanComponentPropertyMetaKey parentKey = new IkasanComponentPropertyMetaKey(parentPropertyName);
//                        IkasanComponentPropertyMeta parent = componentProperties.get(parentKey);
//                        if (parent == null) {
//                            componentProperties.put(parentKey, new IkasanComponentPropertyMeta(newKey, ikasanComponentPropertyMeta));
//                        }
//                        else {
//                            parent.addSubProperty(newKey, ikasanComponentPropertyMeta);
//                        }
//                    } else {
                        // Simple scenario
                        componentProperties.put(newKey, ikasanComponentPropertyMeta);
//                    }
                }
            } catch (IOException ioe) {
                LOG.warn("Could not read the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
            }
        } else {
            LOG.warn("Could not load the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
        }
        return componentProperties;
    }

    protected Element deserializeElement(String elementPath) throws IOException {
        LOG.info("Loading " + elementPath);

        ObjectMapper objectMapper = new ObjectMapper();
        // Below prevents comments in JSON from causing issues.
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(elementPath);
        String jsonString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());
        Element element = objectMapper.readValue(jsonString, Element.class);
        return element;
    }


    private static final int ARTIFACT_ID_INDEX = 0;
    private static final int GROUP_ID_INDEX = 1;
    private static final int VERSION_INDEX = 2;
    private static final int NUMBER_OF_DEPENDENCY_CONFIGS = 3;
    public static final String COMPONENT_DEPENDENCIES_DIR = "/studio/componentDependencies/";

    /**
     * Look for csv files in /studio/componentDependencies and load the Maven dependencies (jar dependencies) for that
     * component e.g. BASIC.csv holds all the basic Ikasan dependencies
     * artifactid,           groupid,   versionid
     * ikasan-connector-base,org.ikasan,3.1.0
     * ikasan-eip-standalone,org.ikasan,3.1.0
     * so the Map is of Dependency.getManagementKey() -> Dependency
     *
     * @param propertiesFile one of the properties fiiles to be read
     * @return the growing list of dependencies, including the properties file provided.
     */
    public static Map<String, Dependency> readIkasanComponentDependencies(String propertiesFile) {

        Map<String, Dependency> dependencies = new TreeMap<>() ;        // Map of Dependency.getManagementKey() -> Dependency

        String propertiesFileName = COMPONENT_DEPENDENCIES_DIR + propertiesFile + ".csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);

        Set<String> artifactIds = new HashSet<>();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }
                    String[] split = line.split(",");
                    if (split.length != NUMBER_OF_DEPENDENCY_CONFIGS) {
                        LOG.warn("An incorrect dependency config has been supplied, incorrect number of configs (should be " + NUMBER_OF_DEPENDENCY_CONFIGS +
                                ", was " + split.length + "), please remove from " + propertiesFile + " or fix, the line was [" + line+ "]");
                        continue;
                    }

                    final String artifactId = split[ARTIFACT_ID_INDEX];
                    String groupId = split[GROUP_ID_INDEX];
                    String version = split[VERSION_INDEX];

                    if (artifactId.isEmpty() || groupId.isEmpty() || version.isEmpty()) {
                        LOG.warn("The dependency is not fully configured, artifactId=" + artifactId + ", groupId=" + groupId + ",version=" + version + " so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                    } else if (artifactIds.contains(artifactId)) {
                        LOG.warn("A property of this propertyConfigLabel already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                    } else {
                        artifactIds.add(artifactId);
                        Dependency dependency = new Dependency();
                        dependency.setType("jar");
                        dependency.setArtifactId(artifactId);
                        dependency.setGroupId(groupId);
                        dependency.setVersion(version);

                        dependencies.put(dependency.getManagementKey(), dependency);
                    }
                }
            } catch (IOException ioe) {
                LOG.warn("Could not read the dependency properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
            }
        } else {
            LOG.warn("Could not open the dependency properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
        }
        return dependencies;
    }

    private static Object getDefaultValue(final String[] split, final Class dataTypeOfProperty, final String line, final String propertiesFile) {
        Object defaultValue = null;
        String defaultValueAsString = split[DEFAULT_VALUE_INDEX];
        if (dataTypeOfProperty != null && defaultValueAsString != null && !defaultValueAsString.isEmpty()) {
            try {
                if ("java.lang.String".equals(split[PROPERTY_DATA_TYPE_INDEX])) {
                    defaultValue = defaultValueAsString;
                } else {
//                    Method methodToFind = dataTypeOfProperty.getMethod("valueOf", null);
                    Method methodToFind = dataTypeOfProperty.getMethod("valueOf", new Class[]{String.class});
                    defaultValue = methodToFind.invoke(defaultValue, defaultValueAsString);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                LOG.warn("An error has occurred while determining the default value for " + line + " please remove from " + propertiesFile + " or correct it. The default value will be set to null ", ex);
            }
        }
        return defaultValue;
    }

    /**
     * Convert the supplied java Object into its JSON representation
     * @param value to be turned to JSON
     * @return the Object in JSON format.
     */
    public static String toJson(Object value) {
        String moduleString = "CouldNotConvert";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            moduleString = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            value = "";
            LOG.warn("Could not generate JSON from [" + value + "] message [" + jpe.getMessage() + "]", jpe);
        }
        return moduleString;
    }

    private static final String SUBSTITUTION_PREFIX = "__";
    private static final String SUBSTITUTION_PREFIX_FLOW = SUBSTITUTION_PREFIX + "flow";
    private static final String SUBSTITUTION_PREFIX_COMPONENT = SUBSTITUTION_PREFIX + "component";
    private static final String SUBSTITUTION_PREFIX_MODULE = SUBSTITUTION_PREFIX + "module";
    public static String getPropertyLabelPackageStyle(Module module, Flow flow, IkasanElement component, String template) {
        String propertyLabel = template;
        if (template != null && template.contains(SUBSTITUTION_PREFIX)) {
            if (module != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_MODULE, module.getJavaPackageName());
            }
            if (flow != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_FLOW, flow.getJavaPackageName());
            }
            if (component != null ) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_COMPONENT, component.getJavaPackageName());
            }
        }
        return propertyLabel;
    }

    public static String getPropertyLabelVariableStyle(Module module, Flow flow, IkasanElement component, String template) {
        String propertyLabel = template;
        if (template != null && template.contains(SUBSTITUTION_PREFIX)) {
            if (module != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_MODULE, module.getJavaVariableName());
            }
            if (flow != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_FLOW, flow.getJavaVariableName());
            }
            if (component != null ) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_COMPONENT, component.getJavaVariableName());
            }
        }
        return StudioUtils.toJavaIdentifier(propertyLabel);
    }
}
