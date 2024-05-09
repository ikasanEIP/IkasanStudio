package org.ikasan.studio.core.model.ikasan.instance;

import com.intellij.openapi.diagnostic.Logger;
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
    private Set<String> dependencyKeys = new HashSet<>(); // Allows us to track what dependencies have already been set

    boolean isDirty = false;
    public IkasanPomModel() {
    }

    public IkasanPomModel(Model model) {
        this.model = model;
        resetKeys(model.getDependencies());
    }

    /**
     * Typically when we are about to persist this model and discard it
     * @return a String representation of the POM
     */
    public String getModelAsString() {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        StringWriter pomStringWriter = new StringWriter();
        try {
            writer.write(pomStringWriter, model);
        } catch (IOException e) {
            LOG.warn("STUDIO: WARN, an error occurred trying to get the pom as a string. Trace: [" +
                    Arrays.toString(e.getStackTrace()) + "]");
        }
        return pomStringWriter.toString();
    }

    /**
     * Add in the new dependency (and set the instance to dirty) only if the dependency is not already added.
     * @param newDependency to add
     * @return true if the dependency did not already exist in the pom
     */
    public boolean checkIfDependancyAlreadyExists(Dependency newDependency) {
        if (!dependencyKeys.contains(getKey(newDependency))) {
            List<Dependency> newRawList = model.getDependencies();
            newRawList.add(newDependency);
            updateModeAndDependencyKeys(newRawList);
            isDirty = true;
        }
        return isDirty;
    }

    public boolean hasDependency(Dependency newDependency) {
        return newDependency != null && dependencyKeys.contains(getKey(newDependency));
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
            resetKeys(dependencyListUniqueSorted);
        }
    }

    private void resetKeys(Collection<Dependency> dependencies) {
        dependencyKeys.clear();
        for(Dependency dependency : dependencies) {
            dependencyKeys.add(getKey(dependency));
        }
    }

    private String getKey(Dependency dependency) {
        return dependency.getGroupId()+":"+dependency.getArtifactId()+":"+dependency.getVersion();
    }

    public boolean isDirty() {
        return isDirty;
    }
}
