package org.ikasan.studio.core.model;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ModelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModelUtils.class);

    public static Set<Dependency> getAllUniqueSortedDependenciesSet(Collection<Dependency> rawDependencies) {
        SortedSet<Dependency> sorted = new TreeSet<>(Comparator.comparing(Dependency::getGroupId)
                .thenComparing(Dependency::getArtifactId)
                .thenComparing(Dependency::getVersion, Comparator.nullsFirst(String::compareTo)));
        Map<String, Dependency> dependenciesByKey = new TreeMap<>();

        if (rawDependencies != null) {
            for (Dependency dependency : rawDependencies) {
                String key = dependency.getManagementKey();
                Dependency existing = dependenciesByKey.get(key);
                if (existing == null) {
                    dependenciesByKey.put(key, dependency);
                    continue;
                }
                String existingVersion = existing.getVersion();
                String candidateVersion = dependency.getVersion();
                if (Objects.equals(existingVersion, candidateVersion)) continue;
                if (candidateVersion == null) {
                    dependenciesByKey.put(key, dependency);
                } else if (existingVersion != null) {
                    throw new StudioBuildRuntimeException("Conflicting dependency versions for " + key
                            + ": " + existingVersion + " and " + candidateVersion);
                }
            }
            sorted.addAll(dependenciesByKey.values());
        }
        return sorted;
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
                if (!Objects.equals(firstNumber, secondNumber)) {
                    if (firstNumber > secondNumber) {
                        break;
                    } else {
                        firstIsNewer = false;
                        break;
                    }
                }
            }
        }
        return firstIsNewer;
    }

    public static Integer safeParse(String number) {
        int value = 0;
        try {
            value = Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            LOG.warn("STUDIO: Could not convert the string [" + number + "] inter a number, trace: " + Arrays.toString(nfe.getStackTrace()));
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
