package org.ikasan.studio.core.model.ikasan;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ComponentPropertyMetaContractTest {
    @Test
    void comparisonAndPatternLookupDoNotInvalidateHashCollections() {
        var first = ComponentPropertyMeta.builder().validation("[a-z]+").build();
        var second = ComponentPropertyMeta.builder().validation("[a-z]+").build();
        var set = new HashSet<ComponentPropertyMeta>();
        set.add(first);
        int originalHash = first.hashCode();
        assertEquals(first, second);
        first.getValidationPattern();
        assertEquals(originalHash, first.hashCode());
        assertTrue(set.contains(first));
        assertTrue(set.contains(second));
    }

    @Test
    void changingOrClearingValidationInvalidatesCompiledPattern() {
        var meta = ComponentPropertyMeta.builder().validation("[a-z]+").build();
        assertTrue(meta.getValidationPattern().matcher("abc").matches());
        meta.setValidation("[0-9]+");
        assertTrue(meta.getValidationPattern().matcher("123").matches());
        assertFalse(meta.getValidationPattern().matcher("abc").matches());
        meta.setValidation("");
        assertNull(meta.getValidationPattern());
        meta.setValidation(null);
        assertNull(meta.getValidationPattern());
    }

    @Test
    void builderCopyUsesTheUpdatedRegex() {
        var original = ComponentPropertyMeta.builder().validation("[a-z]+").build();
        original.getValidationPattern();
        var changed = original.toBuilder().validation("[0-9]+").build();
        assertTrue(changed.getValidationPattern().matcher("123").matches());
        assertTrue(original.getValidationPattern().matcher("abc").matches());
    }
}
