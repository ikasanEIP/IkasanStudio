package org.ikasan.studio.core.model.ikasan.instance;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PayloadTypesTest {
    @Test
    void splitsAlternativesOutsideNestedGenerics() {
        assertEquals(List.of("java.util.Map<String, java.util.List<Integer>>", "byte[]"),
                PayloadTypes.alternatives("java.util.Map<String, java.util.List<Integer>>, byte[]"));
    }

    @Test
    void comparesTypesWithoutSubstringMatches() {
        assertFalse(PayloadTypes.accepts("String", "java.util.List<java.lang.String>"));
        assertFalse(PayloadTypes.accepts("String", "java.lang.StringBuilder"));
        assertFalse(PayloadTypes.accepts("a.Message", "b.Message"));
        assertTrue(PayloadTypes.accepts("java.util.Map", "java.util.HashMap"));
        assertTrue(PayloadTypes.accepts("java.lang.CharSequence", "java.lang.String"));
        assertTrue(PayloadTypes.accepts("String", "java.lang.String"));
    }

    @Test
    void checksGenericArgumentsAndUnknownDeclarations() {
        assertFalse(PayloadTypes.accepts("java.util.List<String>", "java.util.List<Integer>"));
        assertTrue(PayloadTypes.accepts("java.util.List<String>", "java.util.ArrayList<String>"));
        assertTrue(PayloadTypes.accepts("java.util.List<String>", "java.util.List"));
        assertTrue(PayloadTypes.accepts("String", "java.lang.Object (auto-converted)"));
        assertTrue(PayloadTypes.unknown("org.ikasan.spec.event.FlowEvent<String>"));
        assertFalse(PayloadTypes.unknown("java.util.List<Object>"));
    }
}
