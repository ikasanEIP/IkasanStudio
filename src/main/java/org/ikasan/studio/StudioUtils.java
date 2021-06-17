package org.ikasan.studio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.model.ikasan.IkasanComponent;
import org.ikasan.studio.model.ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Studio Utils
 */
public class StudioUtils {
    private static final Logger LOG = Logger.getLogger(StudioUtils.class);

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
        StringBuilder returnString = new StringBuilder("");
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
        if (identifer.length() > 0) {
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
        if (input != null && input.length() > 0) {
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
        if (input != null && input.length() > 0) {
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
        if (input != null && input.length() > 0) {
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
                    Character current;
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
    private static final int MANDATORY_INDEX = 1;
    private static final int USER_IMPLEMENTED_CLASS_INDEX = 2;
    private static final int USER_DEFINED_RESOURCE_INDEX = 3;
    private static final int PROPERTY_CONFIG_LABEL_INDEX = 4;
    private static final int PROPERTY_DATA_TYPE_INDEX = 5;
    private static final int USAGE_DATA_TYPE_INDEX = 6;
    private static final int VALIDATION_INDEX = 7;
    private static final int DEFAULT_VALUE_INDEX = 8;
    private static final int HELP_INDEX = 9;
    private static final int NUMBER_OF_CONFIGS = 10;
    public static final String COMPONENT_DEFINTIONS_DIR = "/studio/componentDefinitions/";

    public static Map<String, IkasanComponentPropertyMeta> readIkasanComponentProperties(String propertiesFile) {
        Map<String, IkasanComponentPropertyMeta> componentProperties = new TreeMap<>();
        componentProperties.put(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT);

        String propertiesFileName = COMPONENT_DEFINTIONS_DIR + propertiesFile + "_en_GB.csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);
        Set<String> propertyConfigLabels = new HashSet<>();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split("\\|");
                    if (split.length != NUMBER_OF_CONFIGS) {
                        LOG.error("An incorrect config has been supplied, incorrect number of configs (should be " + NUMBER_OF_CONFIGS + "), please remove from " + propertiesFile + " or fix, the line was [" + line+ "]");
                        continue;
                    }
                    if (componentProperties.containsKey(split[PROPERTY_NAME_INDEX])) {
                        LOG.error("A property of this name already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                        continue;
                    }

                    boolean isMandatory = Boolean.parseBoolean(split[MANDATORY_INDEX]);
                    boolean isUserImplementedClass = Boolean.parseBoolean(split[USER_IMPLEMENTED_CLASS_INDEX]);
                    boolean isUserDefinedResource = Boolean.parseBoolean(split[USER_DEFINED_RESOURCE_INDEX]);

                    final String propertyConfigLabel = split[PROPERTY_CONFIG_LABEL_INDEX];
                    if (propertyConfigLabel != null && propertyConfigLabel.length() > 0) {
                        if (propertyConfigLabels.contains(propertyConfigLabel)) {
                            LOG.error("A property of this propertyConfigLabel already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                            continue;
                        } else {
                            propertyConfigLabels.add(propertyConfigLabel);
                        }
                    }

                    String usageDataType = split[USAGE_DATA_TYPE_INDEX];
                    String validation = split[VALIDATION_INDEX];

                    // Data type
                    Class propertyDataType;
                    try {
                        propertyDataType = Class.forName(split[PROPERTY_DATA_TYPE_INDEX]);
                    } catch (ClassNotFoundException ex) {
                        LOG.error("An error has occurred while determining the class for " + line + " please remove from " + propertiesFile + " or correct it ", ex);
                        propertyDataType = String.class;  // dont crash the IDE
                    }

                    //  default value
                    Object defaultValue = getDefaultValue(split, propertyDataType, line,  propertiesFile);

                    IkasanComponentPropertyMeta ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(isMandatory, isUserImplementedClass, isUserDefinedResource, split[PROPERTY_NAME_INDEX], propertyConfigLabel, propertyDataType, usageDataType, validation, defaultValue, split[HELP_INDEX]);
                    componentProperties.put(split[PROPERTY_NAME_INDEX], ikasanComponentPropertyMeta);
                }
            } catch (IOException ioe) {
                LOG.warn("Could not read the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
            }
        } else {
            LOG.warn("Could not load the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
        }
        return componentProperties;
    }

    private static final int ARTIFACT_ID_INDEX = 0;
    private static final int GROUP_ID_INDEX = 1;
    private static final int VERSION_INDEX = 2;
    private static final int NUMBER_OF_DEPENDENCY_CONFIGS = 3;
    public static final String COMPONENT_DEPENDENCIES_DIR = "/studio/componentDependencies/";

    public static List<Dependency> readIkasanComponentDependencies(String propertiesFile) {
        List<Dependency> dependencies = new ArrayList<>() ;

        String propertiesFileName = COMPONENT_DEPENDENCIES_DIR + propertiesFile + ".csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);

        Set<String> artifactIds = new HashSet<>();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split(",");
                    if (split.length != NUMBER_OF_DEPENDENCY_CONFIGS) {
                        LOG.error("An incorrect dependency config has been supplied, incorrect number of configs (should be " + NUMBER_OF_DEPENDENCY_CONFIGS + "), please remove from " + propertiesFile + " or fix, the line was [" + line+ "]");
                        continue;
                    }

                    final String artifactId = split[ARTIFACT_ID_INDEX];
                    String groupId = split[GROUP_ID_INDEX];
                    String version = split[VERSION_INDEX];

                    if (artifactId.isEmpty() || groupId.isEmpty() || version.isEmpty()) {
                        LOG.error("The dependency is not fully configured, artifactId=" + artifactId + ", groupId=" + groupId + ",version=" + version + " so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                    } else if (artifactIds.contains(artifactId)) {
                        LOG.error("A property of this propertyConfigLabel already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                    } else {
                        artifactIds.add(artifactId);
                        Dependency dependency = new Dependency();
                        dependency.setType("jar");
                        dependency.setArtifactId(artifactId);
                        dependency.setGroupId(groupId);
                        dependency.setVersion(version);
                        dependencies.add(dependency);
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
        if (dataTypeOfProperty != null && defaultValueAsString != null && defaultValueAsString.length() > 0) {
            try {
                if ("java.lang.String".equals(split[PROPERTY_DATA_TYPE_INDEX])) {
                    defaultValue = defaultValueAsString;
                } else {
//                    Method methodToFind = dataTypeOfProperty.getMethod("valueOf", null);
                    Method methodToFind = dataTypeOfProperty.getMethod("valueOf", new Class[]{String.class});
                    if (methodToFind != null) {
                        defaultValue = methodToFind.invoke(defaultValue, defaultValueAsString);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                LOG.error("An error has occurred while determining the default value for " + line + " please remove from " + propertiesFile + " or correct it. The default value will be set to null ", ex);
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
            LOG.warn("Could not generate JSON from [" + value + "]");
        }
        return moduleString;
    }

    private static final String SUBSTITUTION_PREFIX = "__";
    private static final String SUBSTITUTION_PREFIX_FLOW = SUBSTITUTION_PREFIX + "flow";
    private static final String SUBSTITUTION_PREFIX_COMPONENT = SUBSTITUTION_PREFIX + "component";
    private static final String SUBSTITUTION_PREFIX_MODULE = SUBSTITUTION_PREFIX + "module";
    public static String getPropertyLabelPackageStyle(IkasanModule module, IkasanFlow flow, IkasanComponent component, String template) {
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

    public static String getPropertyLabelVariableStyle(IkasanModule module, IkasanFlow flow, IkasanComponent component, String template) {
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
