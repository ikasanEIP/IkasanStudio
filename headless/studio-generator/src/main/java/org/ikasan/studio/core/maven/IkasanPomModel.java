package org.ikasan.studio.core.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
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
 * For efficiency; we only flush the IDE with the new pom (quite expensive) WHEN there are new changes.
 * </p>
 * Typically, this object is thrown away once its dirty and re-read anew (to safeguard against simultaneous updates)
 */
public class IkasanPomModel {
    private static final Logger LOG = LoggerFactory.getLogger(IkasanPomModel.class);
    public static final String MAVEN_COMPILER_TARGET = "maven.compiler.target";
    public static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";

    Model model;
    private final Map<String, String> dependencyMap = new HashMap<>(); // Allows us to track what dependencies have already been set

    volatile boolean isDirty = false;

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
     * Add a missing dependency or align an existing dependency with the exact meta-pack contract.
     * @param newDependency dependency selected by the meta-pack
     * @return true when the Maven model changed
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean checkIfDependancyAlreadyExists(Dependency newDependency) {
        String newMapKey = getMapKey(newDependency);
        if (!dependencyMap.containsKey(newMapKey)) {
            List<Dependency> dependencies = model.getDependencies();
            dependencies.add(newDependency);
            updateModeAndDependencyKeys(dependencies);
            isDirty = true;
            return true;
        }
        Dependency existing = model.getDependencies().stream()
                .filter(dependency -> newMapKey.equals(getMapKey(dependency)))
                .findFirst().orElse(null);
        if (existing != null && !Objects.equals(existing.getVersion(), newDependency.getVersion())) {
            existing.setVersion(newDependency.getVersion());
            resetKeys(model.getDependencies());
            isDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Check if the supplied dependency is new its name/group is unknown or its verison differs.
     * @param newDependency to check
     * @return true if the 'newDependency' is not already in the pom
     */
    public boolean isNewDependency(Dependency newDependency) {
        return newDependency != null &&
                (!dependencyMap.containsKey(getMapKey(newDependency)) ||
                !Objects.equals(newDependency.getVersion(), dependencyMap.get(getMapKey(newDependency))));
    }

    /**
     * Check if the dependencies are already known.
     * @param newDependencies to check
     * @return true if any of the supplied newDependencies are not already in the pom
     */
    public boolean isNewDependency(Collection<Dependency> newDependencies) {
        boolean isNewDependency = false;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            for (Dependency dependency : newDependencies) {
                if (isNewDependency(dependency)) {
                    isNewDependency = true;
                    break;
                }
            }
        }
        return isNewDependency;
    }

    /**
     * If both key and value are not null, and are either not already present, or there is a newer value
     * then add the key value to the properties
     * @param key to be added
     * @param value to be added
     * @return if the key/value were added
     */
    // Its only current caller (StudioProjectFiles#pomAddStandardProperties) discards this return value - the
    // caller further up the chain checks isDirty() instead once all properties/dependencies have been processed.
    @SuppressWarnings("UnusedReturnValue")
    public boolean addProperty(String key, String value) {
        if (key != null && value != null &&
                (!model.getProperties().containsKey(key) || !value.equals(model.getProperties().getProperty(key)))) {
            model.getProperties().setProperty(key, value);
            isDirty = true;
            return isDirty;
        }
        return false;
    }

    /** Adds or aligns a BOM import without disturbing unrelated dependency-management entries. */
    public void addOrUpdateBomImport(String groupId, String artifactId, String version) {
        DependencyManagement management = model.getDependencyManagement();
        if (management == null) {
            management = new DependencyManagement();
            model.setDependencyManagement(management);
        }
        Dependency existing = management.getDependencies().stream()
                .filter(dependency -> Objects.equals(groupId, dependency.getGroupId())
                        && Objects.equals(artifactId, dependency.getArtifactId()))
                .findFirst().orElse(null);
        if (existing == null) {
            Dependency bom = new Dependency();
            bom.setGroupId(groupId);
            bom.setArtifactId(artifactId);
            bom.setVersion(version);
            bom.setType("pom");
            bom.setScope("import");
            management.addDependency(bom);
            isDirty = true;
            return;
        }
        if (!Objects.equals(version, existing.getVersion())
                || !"pom".equals(existing.getType()) || !"import".equals(existing.getScope())) {
            existing.setVersion(version);
            existing.setType("pom");
            existing.setScope("import");
            isDirty = true;
        }
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
