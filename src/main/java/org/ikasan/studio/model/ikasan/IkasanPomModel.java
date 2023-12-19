package org.ikasan.studio.model.ikasan;

import com.intellij.psi.PsiFile;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Used to model the top level pom for the project
 *
 * As new components are added that might have specific dependencies, the top level pom needs to be updated
 * For efficiency, we only flush the IDE with the new pom (quite expensive) WHEN there are new changes.
 *
 * Typically, this objecy is thrown away once its dirty and re-read anew (to safeguard against simultaneous updates)
 */
public class IkasanPomModel {
    public static final String MAVEN_COMPILER_TARGET = "maven.compiler.target";
    public static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";

    Model model;
    PsiFile pomPsiFile;
    Set<String> dependencyKeys = new HashSet<>(); // Allows us to track what dependencies have already been set

    boolean isDirty = false;
    public IkasanPomModel() {
    }

    public IkasanPomModel(Model model, PsiFile pomPsiFile) {
        this.model = model;
        this.pomPsiFile = pomPsiFile;
        updateDependencyKeys();
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
            e.printStackTrace();
        }
        return pomStringWriter.toString();
    }

    /**
     * Add in the new dependency (and set the instance to dirty) only if the dependancy is not already added.
     * @param newDependency to add
     * @return true if the dependency did not already exist in the pom
     */
    public boolean addDependency(Dependency newDependency) {
        if (! hasDependency(newDependency)) {
            model.addDependency(newDependency);
            dependencyKeys.add(newDependency.getManagementKey());
            isDirty = true;
        }
        return isDirty;
    }

    public boolean hasDependency(Dependency newDependency) {
        return newDependency != null && dependencyKeys.contains(newDependency.getManagementKey());
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

    private void updateDependencyKeys() {
        if (model != null) {
            List<Dependency> dependencyList = model.getDependencies();
            for (Dependency dependency : dependencyList) {
                dependencyKeys.add(dependency.getManagementKey());
            }
        }
    }

    public boolean isDirty() {
        return isDirty;
    }
}
