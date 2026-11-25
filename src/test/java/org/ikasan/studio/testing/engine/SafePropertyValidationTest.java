package org.ikasan.studio.testing.engine;

import java.time.Duration;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("engine")
class SafePropertyValidationTest {
    @Test
    void nestedQuantifiersCannotCauseCatastrophicBacktracking() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            ComponentPropertyMeta meta = ComponentPropertyMeta.builder().validation("(a+)+$").build();
            assertTrue(meta.matchesValidation("a".repeat(100_000)));
            assertFalse(meta.matchesValidation("a".repeat(100_000) + "!"));
            meta.setValidation("(a|aa)+$");
            assertFalse(meta.matchesValidation("a".repeat(100_000) + "!"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"(?=a)a", "(?!a).*", "(?<=a)b", "(a)\\1", "a++", "a{1001}", "["})
    void unsupportedRulesFailInsteadOfFallingBackToJavaRegex(String expression) {
        ComponentPropertyMeta meta = ComponentPropertyMeta.builder().validation(expression).build();
        assertThrows(IllegalArgumentException.class, () -> meta.matchesValidation("aa"));
    }

    @Test
    void boundsExpressionSizeBeforeCompilation() {
        ComponentPropertyMeta meta = ComponentPropertyMeta.builder()
                .validation("a".repeat(ComponentPropertyMeta.MAX_VALIDATION_EXPRESSION_LENGTH + 1)).build();
        assertThrows(IllegalArgumentException.class, meta::getValidationPattern);
    }

    @Test
    void packageRuleChecksEverySegmentAndCombinesWithAnOptionalExpression() {
        ComponentPropertyMeta meta = ComponentPropertyMeta.builder().validationKind(ComponentPropertyMeta.JAVA_PACKAGE).build();
        assertTrue(meta.hasValidation());
        for (String legal : new String[]{"org.example", "com.日本.app", "com.$app", "com.imports", "com.école", "com.𐐀app"})
            assertTrue(meta.matchesValidation(legal), legal);
        for (String illegal : new String[]{"", "com..app", "com.import.app", "com.app.class", "true.app", "com._", "com.null", "com.1app", "com.app\n", "com.my-company"})
            assertFalse(meta.matchesValidation(illegal), illegal);
        meta.setValidation("org\\..*");
        assertTrue(meta.matchesValidation("org.example"));
        assertFalse(meta.matchesValidation("com.example"));
        assertFalse(meta.matchesValidation("org.class"));
    }

    @Test
    void semanticRulesArePartOfMetadataIdentityAndUnknownRulesFailClosed() {
        ComponentPropertyMeta plain = ComponentPropertyMeta.builder().build();
        ComponentPropertyMeta typed = plain.toBuilder().validationKind(ComponentPropertyMeta.JAVA_PACKAGE).build();
        assertNotEquals(plain, typed);
        assertFalse(plain.hasValidation());
        assertTrue(plain.matchesValidation("anything"));
        typed.setValidationKind("TYPO");
        assertThrows(IllegalArgumentException.class, () -> typed.matchesValidation("org.example"));
    }
}
