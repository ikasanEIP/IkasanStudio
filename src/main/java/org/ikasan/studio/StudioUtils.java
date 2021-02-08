package org.ikasan.studio;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Studio Utils
 */
public class StudioUtils {
    private static final Logger log = Logger.getLogger(StudioUtils.class);
    /**
     * Given a string deleimited by tokens e.g. this.is.my.class.bob then get the last string, bob in thei case
     * @param delimeter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input strung to analyse
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
     * Pretty much what org.apache.commons.text.CaseUtils does, but we are limited by what libs Intellij
     * pull into the plugin dependencies.
     * @param input a string potentially with spaces
     * @return
     */
    public static String toJavaIdentifier(final String input) {
        if (input != null && input.length() > 0) {
            int inputStringLength = input.length();
            char inputString[] = input.toCharArray();
            int outputStringLength = 0;

            boolean toUpper = false;
            for (int inputStringIndex = 0; inputStringIndex < inputStringLength; inputStringIndex++)
            {
                if (inputString[inputStringIndex] == ' ') {
                    toUpper = true;
                    continue;
                }
                else {
                    Character current = null;
                    if (outputStringLength == 0) {
                        current = Character.toLowerCase(inputString[inputStringIndex]);
                        if (! (Character.isJavaIdentifierStart(current))) {
                            System.out.println("Noo");
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
                            current = Character.toLowerCase(inputString[inputStringIndex]);
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

    private static int MANDATORY_INDEX = 0;
    private static int NAME_INDEX = 1;
    private static int PROPERTY_CONFIG_LABEL_INDEX = 2;
    private static int CLASS_INDEX = 3;
    private static int DEFAULT_VALUE_INDEX = 4;
    private static int HELP_INDEX = 5;
    private static int NUMBER_OF_CONFIGS = 6;
    private static String COMPONENT_DEFINTIONS_DIR = "/studio/componentDefinitions/";
    public static Map<String, IkasanComponentPropertyMeta> readIkasanComponentProperties(String propertiesFile) {
        Map<String, IkasanComponentPropertyMeta> componentProperties = new TreeMap<>();
        componentProperties.put(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT);

        String propertiesFileName = COMPONENT_DEFINTIONS_DIR + propertiesFile + ".csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);
        Set<String> propertyConfigLabels = new HashSet<>();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split("\\|");
                    if (split.length != NUMBER_OF_CONFIGS) {
                        log.error("An incorrect config has been supplied, incorrect number of configs (should be " + NUMBER_OF_CONFIGS + "), please remove from " + propertiesFile + " or fix, the line was " + line);
                        continue;
                    }
                    if (componentProperties.containsKey(split[NAME_INDEX])) {
                        log.error("A property of this name already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                        continue;
                    }
                    // Mandatory
                    boolean isMandatory = false;
                    if (split[MANDATORY_INDEX].equals("true")) {
                        isMandatory = true;
                    }
                    // propertyConfigLabel
                    final String propertyConfigLabel = split[PROPERTY_CONFIG_LABEL_INDEX];
                    if (propertyConfigLabel != null && propertyConfigLabel.length() > 0) {
                        if (propertyConfigLabels.contains(propertyConfigLabel)) {
                            log.error("A property of this propertyConfigLabel already exists so it will be ignored " + line + " please remove from " + propertiesFile + " or correct it ");
                            continue;
                        } else {
                            propertyConfigLabels.add(propertyConfigLabel);
                        }
                    }

                    // Data type
                    Class dataTypeOfProperty = null;
                    try {
                        dataTypeOfProperty = Class.forName(split[CLASS_INDEX]);
                    } catch (ClassNotFoundException ex) {
                        log.error("An error has occurred while determining the class for " + line + " please remove from " + propertiesFile + " or correct it ", ex);
                        dataTypeOfProperty = String.class;  // dont crash the IDE
                    }

                    //  default value
                    Object defaultValue = getDefaultValue(split, dataTypeOfProperty, line,  propertiesFile);

                    IkasanComponentPropertyMeta ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(isMandatory, split[NAME_INDEX], propertyConfigLabel, dataTypeOfProperty, defaultValue, split[HELP_INDEX]);
                    componentProperties.put(split[NAME_INDEX], ikasanComponentPropertyMeta);
                }
            } catch (IOException ioe) {
                log.warn("Could not read the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
            }
        } else {
            log.warn("Could not read the properties file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
        }
        return componentProperties;
    }

    private static Object getDefaultValue(final String[] split, final Class dataTypeOfProperty, final String line, final String propertiesFile) {
        Object defaultValue = null;
        String defaultValueAsString = split[DEFAULT_VALUE_INDEX];
        if (dataTypeOfProperty != null && defaultValueAsString != null && defaultValueAsString.length() > 0) {
            try {
                if ("java.lang.String".equals(split[CLASS_INDEX])) {
                    defaultValue = defaultValueAsString;
                } else {
                    Method methodToFind = dataTypeOfProperty.getMethod("valueOf", new Class[]{String.class});
                    if (methodToFind != null) {
                        defaultValue = methodToFind.invoke(defaultValue, defaultValueAsString);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                log.error("An error has occurred while determining the default value for " + line + " please remove from " + propertiesFile + " or correct it. The default value will be set to null ", ex);
            }
        }
        return defaultValue;
    }
}
