package org.ikasan.studio.core.generation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceNamesTest {
    @Test
    void appliesExistingGeneratorNamingRules() {
        assertThat(JavaSourceNames.toIdentifier("my flow.name")).isEqualTo("myFlowName");
        assertThat(JavaSourceNames.toClassName("my flow")).isEqualTo("MyFlow");
        assertThat(JavaSourceNames.toPackageName("1 My-Flow")).isEqualTo("_1myflow");
        assertThat(JavaSourceNames.toIdentifier(null)).isEmpty();
    }

    @Test
    void retainsUnusualCharactersThatAreValidInJavaIdentifiers() {
        assertThat(JavaSourceNames.toIdentifier("Δ")).isEqualTo("δ");
        assertThat(JavaSourceNames.toClassName("_special flow")).isEqualTo("_specialFlow");
        assertThat(JavaSourceNames.toPackageName("42-valid.package")).isEqualTo("_42validpackage");
    }
}
