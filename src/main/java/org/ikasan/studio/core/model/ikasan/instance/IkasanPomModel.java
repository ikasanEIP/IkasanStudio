package org.ikasan.studio.core.model.ikasan.instance;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.ikasan.studio.core.model.ModelUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Used to model the top level pom for the project
 * </p>
 * As new components are added that might have specific dependencies, the top level pom needs to be updated
 * For efficiency, we only flush the IDE with the new pom (quite expensive) WHEN there are new changes.
 * </p>
 * Typically, this object is thrown away once its dirty and re-read anew (to safeguard against simultaneous updates)
 */
public class IkasanPomModel {
    private static final Logger LOG = Logger.getInstance("#IkasanPomModel");
    public static final String MAVEN_COMPILER_TARGET = "maven.compiler.target";
    public static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";

    Model model;
    private final Map<String, String> dependencyMap = new HashMap<>(); // Allows us to track what dependencies have already been set

    volatile boolean isDirty = false;

    public IkasanPomModel() {
    }

    public IkasanPomModel(Model model) {
        this.model = model;
        resetKeys(model.getDependencies());
    }

    /**
     * Typically when we are about to persist this model and discard it
     *
     * @return a String representation of the POM
     */
    public String getModelAsString() {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (StringWriter pomStringWriter = new StringWriter()) {
            writer.write(pomStringWriter, model);
            return pomStringWriter.toString();
        } catch (IOException e) {
            LOG.warn("STUDIO: WARN, an error occurred trying to get the pom as a string. Trace: [" +
                    Arrays.toString(e.getStackTrace()) + "]");
        }
        return "";
    }

    /**
     * Add in the new dependency (and set the instance to dirty) only if the dependency is not already added, or is newer
     * @param newDependency to add
     * @return true if the dependency did not already exist in the pom
     */
    public boolean checkIfDependancyAlreadyExists(Dependency newDependency) {
        String newMapKey = getMapKey(newDependency);
        if (!dependencyMap.containsKey(newMapKey) ||
                (dependencyMap.containsKey(newMapKey) && versionIsNewer(newDependency.getVersion(), dependencyMap.get(newMapKey)))) {
            List<Dependency> newRawList = model.getDependencies();
            newRawList.add(newDependency);
            updateModeAndDependencyKeys(newRawList);
            isDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Return true is the Maven version of v1 is newer than v2
     * @param v1 the potentially newer version
     * @param v2 the potentially older version
     * @return true if v1 is newer than v2
     */
    private boolean versionIsNewer(String v1, String v2) {
        ComparableVersion cv1 = new ComparableVersion(v1);
        ComparableVersion cv2 = new ComparableVersion(v2);
        return cv1.compareTo(cv2) > 0;
    }

    public boolean hasDependency(Dependency newDependency) {
        return newDependency != null && dependencyMap.containsKey(getMapKey(newDependency));
    }

    /**
     * Check if the dependencies are already known.
     * @param newDependencies to check
     * @return true if all the provided jar dependenies are already in IkasanPomModel
     */
    public boolean hasDependency(Collection<Dependency> newDependencies) {
        boolean hasDependency = true;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            for (Dependency dependency : newDependencies) {
                if (!hasDependency(dependency)) {
                    hasDependency = false;
                    break;
                }
            }
        }
        return hasDependency;
    }

    /**
     * If both key and value are not null, and are either not already present, or there is a newer value
     * then add the key value to the properties
     * @param key to be added
     * @param value to be added
     * @return if the key/value were added
     */
    public boolean addProperty(String key, String value) {
        if (key != null && value != null &&
                (!model.getProperties().containsKey(key) || !value.equals(model.getProperties().getProperty(key)))) {
            model.getProperties().setProperty(key, value);
            isDirty = true;
            return isDirty;
        }
        return false;
    }

    public String getProperty(String key) {
        return model.getProperties().getProperty(key);
    }

    private void updateModeAndDependencyKeys(List<Dependency> rawDependencies) {
        if (rawDependencies != null) {
            Set<Dependency> sortedUniqueDependencies = ModelUtils.getAllUniqueSortedDependenciesSet(rawDependencies);
            List<Dependency> dependencyListUniqueSorted = new ArrayList<>(sortedUniqueDependencies);
            model.setDependencies(dependencyListUniqueSorted);
            if (dependencyListUniqueSorted != null) {
                resetKeys(dependencyListUniqueSorted);
            } else {
                LOG.warn("STUDIO: WARN, dependencyListUniqueSorted is null.");
            }
        } else {
            LOG.warn("STUDIO: WARN, rawDependencies is null.");
        }
    }

    private void resetKeys(Collection<Dependency> dependencies) {
        if (dependencies != null) {
            dependencyMap.clear();
            for (Dependency dependency : dependencies) {
                if (dependency != null) {
                    dependencyMap.put(getMapKey(dependency), dependency.getVersion());
                } else {
                    LOG.warn("STUDIO: WARN, dependency is null.");
                }
            }
        } else {
            LOG.warn("STUDIO: WARN, dependencies is null.");
        }
    }

    private String getMapKey(Dependency dependency) {
        return dependency.getGroupId()+":"+dependency.getArtifactId();
    }

    public boolean isDirty() {
        return isDirty;
    }
}
