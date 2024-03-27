package org.ikasan.studio.core.model;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ModelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModelUtils.class);
    public static Module generateModuleInstanceFromString(String json, String path) {
        Module newModule;
        try {
            newModule = ComponentIO.deserializeModuleInstanceString(json, path);
        } catch (StudioBuildException e) {
            LOG.warn("Could not read module json");
            throw new RuntimeException(e);
        }
        return newModule;
    }

    public static Set<Dependency> getAllUniqueSortedDependenciesSet(Collection<Dependency> rawDependencies) {
        SortedSet<Dependency> allUniqueSortedJarDepedencies =  new TreeSet<>(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId).thenComparing(Dependency::getVersion));
        Map<String, Dependency> allJarDepedenciesMap =  new TreeMap<>();

        if (rawDependencies != null) {
            for(Dependency dependency : rawDependencies) {
                // Keep the newest version
                if (!allJarDepedenciesMap.containsKey(dependency.getManagementKey())) {
                    allJarDepedenciesMap.put(dependency.getManagementKey(), dependency);
                } else {
                    String inMapVersion = allJarDepedenciesMap.get(dependency.getManagementKey()).getVersion();
                    String queryVersion = dependency.getVersion();
                    // The version is not always a number, sometimes it's a property in which case we can just add it
                    if (allJarDepedenciesMap.containsKey(dependency.getManagementKey()) && !inMapVersion.contains("$") && !queryVersion.contains("$")) {
                        if (firstVersionNewer(queryVersion, inMapVersion)) {
                            allJarDepedenciesMap.put(dependency.getManagementKey(), dependency);
                        }
                    } else {
                        allJarDepedenciesMap.put(dependency.getManagementKey(), dependency);
                    }
                }
            }
            allUniqueSortedJarDepedencies.addAll(allJarDepedenciesMap.values());
        }
        return allUniqueSortedJarDepedencies;
    }

    /**
     * Given 2 string that contain maven style version numbers, determine latest
     * @param firstVersion to check
     * @param secondVersion to check
     * @return on if first is newer than second
     */
    public static boolean firstVersionNewer(String firstVersion, String secondVersion) {
        boolean firstIsNewer = true;
        if (firstVersion!= null && secondVersion != null) {
            String[] first = firstVersion.split("\\.");
            String[] second = secondVersion.split("\\.");
            int lastIndex = Math.max(first.length, second.length);
            for (int index = 0; index < lastIndex; index++) {
                Integer firstNumber = 0;
                Integer secondNumber = 0;
                if (index < first.length) {
                    firstNumber = safeParse(first[index]);
                }
                if (index < second.length) {
                    secondNumber = safeParse(second[index]);
                }
                if (Objects.equals(firstNumber, secondNumber)) {
                    continue;
                } else if (firstNumber > secondNumber) {
                    break;
                } else {
                    firstIsNewer = false;
                    break;
                }
            }
        }
        return firstIsNewer;
    }

    public static Integer safeParse(String number) {
        Integer value = 0;
        try {
            value = Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            LOG.warn("Could not convert the string [" + number + "] inter a number");
        }
        return value;
    }

    /**
     * Remove the start and end quotes from a String to prevent double quoting.
     * @param value to be examined
     * @return the string with the start and end quites removed if there were any present
     */
    public static String stripStartAndEndQuotes(String value) {
        if (value != null && !value.isEmpty()) {
            if (value.startsWith("\"")) {
                value = value.substring(1);
            }
            if (value.endsWith("\"")) {
                value = value.substring(0,value.length()-1);
            }
        }
        return value;
    }
}
