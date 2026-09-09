package org.ikasan.studio.testing.packs;

import java.util.stream.Stream;
import java.util.List;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reviewed expectations for official packs, independent of the descriptors being tested. */
public final class PackExpectations {
    public static final String META_IKASAN_PACK_3_3_9 = "V3.3.9";
    public static final String META_IKASAN_PACK_4_1_6 = "V4.1.6";
    private PackExpectations() { }
    public static Stream<String> metaPacksToTest() {
        return Stream.of(META_IKASAN_PACK_3_3_9, META_IKASAN_PACK_4_1_6);
    }

    /** Explicit expectations: a new pack needs a reviewed namespace rather than inheriting V4 assumptions. */
    public static String enterpriseNamespace(String metaPackVersion) {
        return switch (metaPackVersion) {
            case META_IKASAN_PACK_3_3_9 -> "javax";
            case META_IKASAN_PACK_4_1_6 -> "jakarta";
            default -> throw new IllegalArgumentException("No test namespace defined for " + metaPackVersion);
        };
    }

    public static void assertXmlConverterDependencies(Module module, String metaPackVersion) {
        String[] jaxbDependencies = switch (metaPackVersion) {
            case META_IKASAN_PACK_3_3_9 -> new String[] {
                    "org.ikasan:ikasan-component-converter:compile",
                    "javax.xml.bind:jaxb-api:compile", "com.sun.xml.bind:jaxb-impl:compile"};
            case META_IKASAN_PACK_4_1_6 -> new String[] {
                    "org.ikasan:ikasan-component-converter:compile",
                    "jakarta.xml.bind:jakarta.xml.bind-api:compile", "org.glassfish.jaxb:jaxb-runtime:compile"};
            default -> throw new IllegalArgumentException("No JAXB expectations for " + metaPackVersion);
        };
        assertDependencies(module, metaPackVersion, jaxbDependencies);
    }

    /** Assert coordinates and scope: a wrong dependency can leave the total count unchanged. */
    public static void assertDependencies(Module module, String metaPackVersion, String... componentDependencies) {
        java.util.Set<String> expected = new java.util.TreeSet<>(List.of(
                "org.ikasan:ikasan-eip-standalone:compile",
                "org.ikasan:ikasan-h2-standalone-persistence:compile",
                "org.ikasan:ikasan-test-endpoint:compile"));
        switch (metaPackVersion) {
            case META_IKASAN_PACK_3_3_9 -> { }
            case META_IKASAN_PACK_4_1_6 -> expected.add("org.ikasan:ikasan-test:test");
            default -> throw new IllegalArgumentException("No dependency expectations for " + metaPackVersion);
        }
        expected.addAll(List.of(componentDependencies));
        java.util.Set<String> actual = module.getAllUniqueSortedJarDependencies().stream()
                .map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                        + (dependency.getScope() == null ? "compile" : dependency.getScope()))
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertEquals(expected, actual, "Dependencies for " + metaPackVersion);
    }

}
