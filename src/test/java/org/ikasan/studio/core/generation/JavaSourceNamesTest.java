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

    @Test
    void javaKeywordsAreEscapedWhereTheyWouldBeVariableOrPackageNames() {
        // "Default" or "Import" is a plausible flow name, but default/import cannot be a variable or package.
        assertThat(JavaSourceNames.toVariableName("Default")).isEqualTo("default_");
        assertThat(JavaSourceNames.toVariableName("New")).isEqualTo("new_");
        assertThat(JavaSourceNames.toPackageName("Import")).isEqualTo("import_");
        assertThat(JavaSourceNames.toPackageName("Enum")).isEqualTo("enum_");
        // Class names are capitalised so are never keywords: they must stay exactly as they were.
        assertThat(JavaSourceNames.toClassName("Default")).isEqualTo("Default");
        assertThat(JavaSourceNames.toClassName("import")).isEqualTo("Import");
        assertThat(JavaSourceNames.toIdentifier("Default")).isEqualTo("default");
        // Ordinary names, including non-keyword lookalikes, are untouched.
        assertThat(JavaSourceNames.toVariableName("my flow")).isEqualTo("myFlow");
        assertThat(JavaSourceNames.toVariableName("Defaults")).isEqualTo("defaults");
        assertThat(JavaSourceNames.toPackageName("Imports")).isEqualTo("imports");
    }

    @Test
    void namesWithNoUsableCharactersStillGiveAValidIdentifier() {
        // Previously these produced an empty class name, i.e. "public class  {".
        assertThat(JavaSourceNames.toClassName("123")).isEqualTo("_123");
        assertThat(JavaSourceNames.toIdentifier("123")).isEqualTo("_123");
        assertThat(JavaSourceNames.toClassName("-")).isEqualTo("Unnamed");
        // A name that already yields something keeps that exact result (no renaming of existing output).
        assertThat(JavaSourceNames.toIdentifier("1st Step")).isEqualTo("stStep");
        assertThat(JavaSourceNames.toIdentifier("")).isEmpty();
    }

    @Test
    void aNameWithNoAsciiLettersStillGivesANonEmptyStablePackage() {
        // Previously "" - which generates "package org.ikasan.studio.boot.flow.;".
        String package1 = JavaSourceNames.toPackageName("\u65e5\u672c\u8a9e");
        assertThat(package1).matches("_[0-9a-f]{1,8}");
        assertThat(JavaSourceNames.toPackageName("\u65e5\u672c\u8a9e")).isEqualTo(package1);
        assertThat(JavaSourceNames.toPackageName("\u4e2d\u6587")).isNotEqualTo(package1);
        // Names with any ASCII letters keep exactly the package they had.
        assertThat(JavaSourceNames.toPackageName("Caf\u00e9")).isEqualTo("caf");
        assertThat(JavaSourceNames.toPackageName("My-Flow")).isEqualTo("myflow");
    }
}
