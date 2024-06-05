package org.ikasan.studio.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Studio Utils
 */
public class StudioBuildUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StudioBuildUtils.class);

    // Enforce as a utility only class
    private StudioBuildUtils() {}

    /**
     * Given a string delimited by tokens e.g. this.is.my.class.bob then get the last string, bob in this case
     * @param delimiter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input string to analyse
     * @return The last stoken of the string or an empty sp
     */
    public static String getLastToken(String delimiter, String input) {
        String returnString = "";
        if (input != null && delimiter != null) {
            String [] tokens = input.split(delimiter, -1);
            if (tokens.length > 0) {
                returnString = tokens[tokens.length-1];
            }
        }
        return returnString;
    }

    /**
     * Given a string delimited by tokens e.g. this.is.my.class.bob then get all but the last string, this.is.my.class in this case
     * @param delimiter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input string to analyse
     * @return All but the last stoken of the string or an empty sp
     */
    public static String getAllButLastToken(String delimiter, String input) {
        StringBuilder returnString = new StringBuilder();
        if (input != null && delimiter != null) {
            String [] tokens = input.split(delimiter, -1);
            if (tokens.length > 1) {
                returnString.append(tokens[0]);
                for(int ii = 1; ii < tokens.length-1 ; ii++) {
                    returnString.append(".").append(tokens[ii]);
                }
            }
        }
        return returnString.toString();
    }

    /**
     * Used by FTL, don't assume unused.
     * Like camel case but starts with upper case letter
     * @param input to be changed
     * @return pascal case version of input
     */
    public static String toPascalCase(final String input) {
        return toJavaClassName(input);
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
     *   space and underscore replaced by -
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
     * @return a string that could be used as a java identifier
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

    /**
     * Get the subdirectories of a given directory on the classpath, when in a jar file or file system
     * @param dir to look through
     * @return a string array of subdirectories
     * @throws URISyntaxException if there were issues
     * @throws IOException if there were issues
     */
    public static String[] getDirectories(final String dir) throws URISyntaxException, IOException {
        String[] directoriesNames = new String[0];

        URL url = StudioBuildUtils.class.getClassLoader().getResource(dir);
        if (url == null) {
            LOG.warn("STUDIO: WARNING: Could not find any directory " + dir);
        } else {
            final URI uri = url.toURI();
            Path myPath;
            directoriesNames = new String[0];
            if (uri.getScheme().equals("jar")) {
                // The fileSystem must remain open for Intellij, i.e. we can't close() it.
                FileSystem fileSystem = null;
                try {
                    fileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException fsnf) {
                    LOG.info("STUDIO: " + dir + " is not currently open, attempting to create newFileSystem for it");
                }
                if (fileSystem == null) {
                    fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
                myPath = fileSystem.getPath(dir);
                // The walk must remain open for Intellij
                Set<String> directories = Files.walk(myPath, 1)
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .filter(string -> !string.endsWith(dir))
                    .collect(Collectors.toSet());
                directoriesNames = directories.toArray(String[]::new);
            } else {
                File file = new File(uri);
                String[] fileList = file.list((current, name) -> new File(current, name).isDirectory());
                if (fileList != null) {
                    directoriesNames = Arrays.stream(fileList).map(theFile -> dir + "/" + theFile).toArray(String[]::new);
                }
            }
        }
        return directoriesNames;
    }

    public static final String SUBSTITUTION_PREFIX = "__";
    private static final String SUBSTITUTION_PREFIX_FLOW = "__flow";
    private static final String SUBSTITUTION_PREFIX_COMPONENT = "__component";
    private static final String SUBSTITUTION_PREFIX_MODULE = "__module";

    @JsonIgnore
    public static boolean valueIsAPlaceholder(String value) {
        return value.startsWith(SUBSTITUTION_PREFIX);
    }

    /**
     * ** Used by FTL ***
     * The supplied string template e.g. __flow.ftp.consumer.cron-expression, replacing meta tags so that the final
     * string represents a call to a property e.f. myFlow.ftp.consumer.cron-expression
     * @param module in scope that might relate to this property
     * @param flow in scope that might relate to this property
     * @param ikasanBasicElement in scope that might relate to this property
     * @param template to be updated
     * @return A string representing a property
     */
    public static String substitutePlaceholderInLowerCase(Module module, Flow flow, BasicElement ikasanBasicElement, String template) {
        String propertyLabel = template;
        if (template != null && template.contains(SUBSTITUTION_PREFIX)) {
            if (module != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_MODULE, module.getJavaPackageName());
            }
            if (flow != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_FLOW, flow.getJavaPackageName());
            }
            if (ikasanBasicElement != null ) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_COMPONENT, ikasanBasicElement.getJavaPackageName());
            }
        }
        return propertyLabel;
    }

    /**
     * ** Used by FTL ***
     * The supplied string template e.g. __flow.ftp.consumer.cron-expression, replacing meta tags so that the final
     * string represents a call to a property e.f. myFlow.ftp.consumer.cron-expression
     * @param module in scope that might relate to this property
     * @param flow in scope that might relate to this property
     * @param ikasanBasicElement in scope that might relate to this property
     * @param template to be updated
     * @return A string representing a property
     */
    public static String substitutePlaceholderInPascalCase(Module module, Flow flow, BasicElement ikasanBasicElement, String template) {
        String propertyLabel = template;
        if (template != null && template.contains(SUBSTITUTION_PREFIX)) {
            if (module != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_MODULE, toPascalCase(module.getName()));
            }
            if (flow != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_FLOW, toPascalCase(flow.getName()));
            }
            if (ikasanBasicElement != null ) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_COMPONENT, toPascalCase(ikasanBasicElement.getName()));
            }
        }
        return propertyLabel;
    }

    /**
     * For the given component, if the value of default is a property placeholder, replace it wuith  real value.
     * @param module that holds the components.
     * @param flow that contains the component.
     * @param ikasanBasicElement itself.
     */
    public static void substituteAllPlaceholderInPascalCase(Module module, Flow flow, BasicElement ikasanBasicElement) {
        for (ComponentProperty componentProperty : ikasanBasicElement.getComponentProperties().values()) {
            if (StudioBuildUtils.valueIsAPlaceholder(componentProperty.getValueString())) {
                componentProperty.setValue(substitutePlaceholderInPascalCase(module, flow, ikasanBasicElement, componentProperty.getValueString()));
            }
            if (componentProperty.getMeta().getDefaultValue() != null && StudioBuildUtils.valueIsAPlaceholder(componentProperty.getMeta().getDefaultValue().toString())) {
                ComponentPropertyMeta newMeta = componentProperty.getMeta().toBuilder().build();
                componentProperty.setMeta(newMeta);
                newMeta.setDefaultValue(StudioBuildUtils.substitutePlaceholderInPascalCase(module, flow, ikasanBasicElement, newMeta.getDefaultValue().toString()));
            }
        }
    }

    /**
     * ** Used by FTL ***
     * The supplied string template e.g. __flow.ftp.consumer.cron-expression, replacing meta tags so that the final
     * string represents a call to a property e.f. myFlow.ftp.consumer.cron-expression
     * @param module in scope that might relate to this property
     * @param flow in scope that might relate to this property
     * @param ikasanBasicElement in scope that might relate to this property
     * @param template to be updated
     * @return A string representing a property
     */
    public static String substitutePlaceholderInJavaCamelCase(Module module, Flow flow, BasicElement ikasanBasicElement, String template) {
        String propertyLabel = template;
        if (template != null && template.contains(SUBSTITUTION_PREFIX)) {
            if (module != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_MODULE, module.getJavaVariableName());
            }
            if (flow != null) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_FLOW, flow.getJavaVariableName());
            }
            if (ikasanBasicElement != null ) {
                propertyLabel = propertyLabel.replace(SUBSTITUTION_PREFIX_COMPONENT, ikasanBasicElement.getJavaVariableName());
            }
            // By replacing _ with space, we should get UpperCase after the space, and space removed.
            propertyLabel = propertyLabel.replace("-", " ");
            propertyLabel = StudioBuildUtils.toJavaIdentifier(propertyLabel);
        }
        return propertyLabel;
    }

    /**
     * Turn the comma seperated string and turn to list
     * @param commaSeperatedList to convert
     * @return List representing input string
     */
    public static List<String> stringToList(String commaSeperatedList) {
        List<String> returnList = new ArrayList<>();

        if (commaSeperatedList != null) {
            commaSeperatedList = commaSeperatedList.replace("[", "").replace("]", "");
            List<String> rawList = Arrays.asList(commaSeperatedList.split("\\s*,\\s*"));
            Set<String> deduplicate = new HashSet<>(rawList);
            if (rawList.size() > deduplicate.size()) {
                returnList = new ArrayList<>(deduplicate);
            } else {
                returnList = rawList;
            }
        }
        return returnList;
    }
}
