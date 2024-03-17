package org.ikasan.studio.core.model;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModelUtilsTest {

    @Test
    public void test_getAllUniqueSortedDependenciesSet_theNewerVersionOnlyIsRetained() {
        Dependency d1 = createDependency("org", "bob", "1.1.1");
        Dependency d2 = createDependency("org", "bob", "1.1.0");
        Dependency d3 = createDependency("org", "bob", "1.1.1");

        Set<Dependency> actual = ModelUtils.getAllUniqueSortedDependenciesSet(Arrays.asList(d1, d2, d3));
        Object[] actualArray = actual.toArray();
        assertAll(
                "Check set is unique",
                () -> assertEquals(1, actual.size()),
                () -> assertEquals("1.1.1", ((Dependency)actualArray[0]).getVersion())
        );
    }
    @Test
    public void testVersionNumber() {
        assertAll(
                "Check first version is greater than second",
                () -> assertTrue(ModelUtils.firstVersionNewer("1", "0")),
                () -> assertTrue(ModelUtils.firstVersionNewer("1.1", "1")),
                () -> assertTrue(ModelUtils.firstVersionNewer("1.1.1", "1.1.0")),
                () -> assertFalse(ModelUtils.firstVersionNewer("1.1.0", "1.1.1")),
                () -> assertFalse(ModelUtils.firstVersionNewer("1.1.0", "1.1.1")),
                () -> assertTrue(ModelUtils.firstVersionNewer("1.1.0.0.1", "1.1.0.0.0")),
                () -> assertTrue(ModelUtils.firstVersionNewer("2.1.0.0.1", "")),
                () -> assertTrue(ModelUtils.firstVersionNewer("2.1.0.0.1", "1")),
                () -> assertFalse(ModelUtils.firstVersionNewer("2", "2.0.1")),
                () -> assertTrue(ModelUtils.firstVersionNewer("1.1.0.0.1", "1.1.0.0.0-RC"))
        );
    }

    private Dependency createDependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

}