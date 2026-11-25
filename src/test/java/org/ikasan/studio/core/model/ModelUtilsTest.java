package org.ikasan.studio.core.model;

import java.util.Arrays;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelUtilsTest {

    @Test
    public void conflictingExplicitVersionsAreRejected() {
        Dependency d1 = createDependency("1.1.1");
        Dependency d2 = createDependency("1.1.0");

        assertThrows(StudioBuildRuntimeException.class,
                () -> ModelUtils.getAllUniqueSortedDependenciesSet(Arrays.asList(d1, d2)));
    }

    @Test
    void bomManagedDependencyWinsOverAnExplicitVersion() {
        Dependency explicit = createDependency("1.1.1");
        Dependency managed = createDependency(null);

        Set<Dependency> actual = ModelUtils.getAllUniqueSortedDependenciesSet(Arrays.asList(explicit, managed));

        assertEquals(1, actual.size());
        assertNull(actual.iterator().next().getVersion());
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

    private Dependency createDependency(String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId("org");
        dependency.setArtifactId("bob");
        dependency.setVersion(version);
        return dependency;
    }

}