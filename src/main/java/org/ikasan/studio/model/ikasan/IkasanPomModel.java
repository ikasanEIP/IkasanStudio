package org.ikasan.studio.model.ikasan;

import com.intellij.psi.PsiFile;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IkasanPomModel {
    Model model;
    PsiFile pomPsiFile;
    Set<String> dependencyKeys = new HashSet<>(); // Allows us to track what dependencies have already been set

    public IkasanPomModel() {
    }

    public IkasanPomModel(Model model, PsiFile pomPsiFile) {
        this.model = model;
        this.pomPsiFile = pomPsiFile;
        updateDependencyKeys();
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
        updateDependencyKeys();
    }

    public void addDependency(Dependency newDependency) {
        model.addDependency(newDependency);
        dependencyKeys.add(newDependency.getManagementKey());
    }

    private void updateDependencyKeys() {
        if (model != null) {
            List<Dependency> dependencyList = model.getDependencies();
            for (Dependency dependency : dependencyList) {
                dependencyKeys.add(dependency.getManagementKey());
            }
        }
    }

    public boolean hasDependencies() {
        return ! dependencyKeys.isEmpty();
    }

    public Set<String> getDependencyKeys() {
        return dependencyKeys;
    }

    /**
     * determine if all of the supplied dependency keys (i.e. set of dependency.getManagementKey()) are already in the model
     * @param newDependencyKeys to check exist
     * @return true if any of the newDependencyKeys are NOT in the existing pom
     */
    public boolean containsAllDependencies(Set<String> newDependencyKeys) {
        return newDependencyKeys != null && dependencyKeys.containsAll(newDependencyKeys);
    }

    public PsiFile getPomPsiFile() {
        return pomPsiFile;
    }

    public void setPomPsiFile(PsiFile pomPsiFile) {
        this.pomPsiFile = pomPsiFile;
    }
}
