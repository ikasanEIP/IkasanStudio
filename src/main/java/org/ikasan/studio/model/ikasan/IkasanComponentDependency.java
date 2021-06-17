package org.ikasan.studio.model.ikasan;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.StudioUtils;

import java.util.List;

public enum IkasanComponentDependency {
    BASIC(),
    JMS();

    List<Dependency> dependencies;

    IkasanComponentDependency() {
        dependencies = StudioUtils.readIkasanComponentDependencies(this.toString());
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
