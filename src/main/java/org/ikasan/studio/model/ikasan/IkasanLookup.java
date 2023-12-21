package org.ikasan.studio.model.ikasan;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public enum IkasanLookup {
    EXCEPTION_RESOLVER_STD_EXCEPTIONS;
    final Map<String,String> displayAndValuePairs;

    IkasanLookup() {
        displayAndValuePairs = readIkasanLookups(this.toString());
    }

    public Map<String, String> getDisplayAndValuePairs() {
        return displayAndValuePairs;
    }

    // --- Population ----
    // Remember, to make the below available to constructor, they must be instance and not static
    private static final Logger LOG = Logger.getInstance("#IkasanLookup");
    private static final int LOOKUP_NAME = 0;
    private static final int LOOKUP_VALUE = 1;
    private static final int NUMBER_OF_CONFIGS = 2;
    public static final String COMPONENT_DEFINTIONS_DIR = "/studio/lookup/";

    public Map<String, String> readIkasanLookups(String propertiesFile) {
        Map<String, String> lookup = new TreeMap<>();

        String propertiesFileName = COMPONENT_DEFINTIONS_DIR + propertiesFile + "_en_GB.csv";
        InputStream is = StudioUtils.class.getResourceAsStream(propertiesFileName);
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }
                    String[] split = line.split("\\|");
                    if (split.length != NUMBER_OF_CONFIGS) {
                        LOG.warn("An incorrect lookup has been supplied, incorrect number of configs (should be " + NUMBER_OF_CONFIGS +
                                ", was " + split.length + "), please remove from properties file [" + propertiesFile + "] or fix, the line was [" + line+ "]");
                        continue;
                    }
                    lookup.put(split[LOOKUP_NAME], split[LOOKUP_VALUE]);
                }
            } catch (IOException ioe) {
                LOG.warn("Could not read the lookup file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
            }
        } else {
            LOG.warn("Could not load the lookup file for " + propertiesFileName + ". This is a non-fatal issues but should be rectified.");
        }
        return lookup;
    }

    /**
     * Given the supplied className, attempt to find a lookup that matches, otherwise return the supplied string
     * @param className to lookup
     * @return the fully qualified lookup or the input string if nothing was found
     */
    public String parseClass(String className) {
        String fullyQualifiedClass = className;
        if (className != null &&
            className.chars().filter(c -> c == '.').count() <2 &&
            className.contains(".class")) {
            String lookup = this.getDisplayAndValuePairs().get(className.replace(".class", ""));
            if (lookup != null) {
                fullyQualifiedClass = lookup;
            } else {
                fullyQualifiedClass = this.getDisplayAndValuePairs().values().stream().filter(x -> x.endsWith(className)).findFirst().orElse(fullyQualifiedClass);
            }
        }
        return fullyQualifiedClass;
    }
}
