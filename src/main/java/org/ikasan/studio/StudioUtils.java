package org.ikasan.studio;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
    private static int HELP_INDEX = 4;
    private static String COMPONENT_DEFINTIONS_DIR = "/studio/componentDefinitions/";
    public static Map<String, IkasanComponentPropertyMeta> readIkasanComponentProperties(String propertiesFile) {
        Map<String, IkasanComponentPropertyMeta> componentProperties = new TreeMap<>();
        componentProperties.put(IkasanComponentPropertyMeta.NAME, IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT);

        String propertiesFileName = COMPONENT_DEFINTIONS_DIR + propertiesFile + ".csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split("\\|");
                    if (split.length != 5) {
                        log.error("An incorrect config has been supplied, please remove from " + propertiesFile + " or fix, line was " + line);
                        continue;
                    }
                    boolean isMandatory = false;
                    if (split[MANDATORY_INDEX].equals("true")) {
                        isMandatory = true;
                    }
                    Class clazz = null;
                    try {
                        clazz = Class.forName(split[CLASS_INDEX]);
                    } catch (ClassNotFoundException ex) {
                        log.error("An error has occurred while determining the class for " + line + " please remove from " + propertiesFile + " or correct it ", ex);
                        clazz = String.class;  // dont crash the IDE
                    }
                    IkasanComponentPropertyMeta ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(isMandatory, split[NAME_INDEX], split[PROPERTY_CONFIG_LABEL_INDEX], clazz, split[HELP_INDEX]);
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
}
