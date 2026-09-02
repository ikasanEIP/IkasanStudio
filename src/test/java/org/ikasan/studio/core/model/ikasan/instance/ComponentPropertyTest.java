package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentPropertyTest {

    /**
     * Regression test for a real production bug: a List-valued property (e.g. Local File Consumer's
     * "filenames") previously rendered via Arrays.toString() - "[myFile\.txt, anotherFile\.txt]" - into
     * application.properties. Spring Boot's comma-splitting List<String> binding then rebuilt that single
     * string into two broken entries ("[myFile\.txt" and " anotherFile\.txt]"), the first of which isn't a
     * valid regex, crashing FileMatcher with a PatternSyntaxException at module startup.
     */
    @Test
    public void getValueString_forListValue_isPlainCommaJoinedWithNoBracketsOrSpaces() {
        ComponentProperty componentProperty = new ComponentProperty(null, List.of("myFile\\.txt", "anotherFile\\.txt"));
        assertEquals("myFile\\.txt,anotherFile\\.txt", componentProperty.getValueString());
    }

    @Test
    public void getValueString_forStringValue_isUnchanged() {
        ComponentProperty componentProperty = new ComponentProperty(null, "myFile\\.txt,anotherFile\\.txt");
        assertEquals("myFile\\.txt,anotherFile\\.txt", componentProperty.getValueString());
    }

    @Test
    public void getValueString_forNullValue_isTheStringNull() {
        ComponentProperty componentProperty = new ComponentProperty(null, null);
        assertEquals("null", componentProperty.getValueString());
    }

    private static ComponentPropertyMeta listPropertyMeta() {
        return ComponentPropertyMeta.builder()
            .propertyName("toRecipients")
            .usageDataType("java.util.List<String>")
            .build();
    }

    /**
     * Regression test: an old, now-fixed bug could persist the literal 2-character string "[]" as a List
     * property's value in model.json (e.g. Email Producer's toRecipients) - java.util.List#toString() of an
     * empty list, saved as a raw String rather than treated as unset. That value survives JSON round-tripping
     * unchanged, so it must be recognised here as "not set", matching every other genuinely-empty field type -
     * see also componentFactory_en.ftl / propertiesTemplate_en.ftl, both of which gate on valueNotSet() rather
     * than a raw null check for exactly this reason.
     */
    @Test
    public void valueNotSet_forStaleEmptyListLiteral_isTrue() {
        ComponentProperty componentProperty = new ComponentProperty(listPropertyMeta(), "[]");
        assertTrue(componentProperty.valueNotSet());
    }

    @Test
    public void valueNotSet_forStaleEmptyListLiteralWithWhitespace_isTrue() {
        ComponentProperty componentProperty = new ComponentProperty(listPropertyMeta(), " [] ");
        assertTrue(componentProperty.valueNotSet());
    }

    @Test
    public void valueNotSet_forEmptyList_isTrue() {
        ComponentProperty componentProperty = new ComponentProperty(listPropertyMeta(), new ArrayList<String>());
        assertTrue(componentProperty.valueNotSet());
    }

    @Test
    public void valueNotSet_forGenuinelyPopulatedList_isFalse() {
        ComponentProperty componentProperty = new ComponentProperty(listPropertyMeta(), List.of("one@example.com"));
        assertFalse(componentProperty.valueNotSet());
    }

    @Test
    public void valueNotSet_forLiteralBracketsOnANonListProperty_isFalse() {
        // "[]" is only ever stale/meaningless for a List-typed property - for any other type it's a genuine,
        // if unusual, String value and must not be silently discarded.
        ComponentPropertyMeta stringMeta = ComponentPropertyMeta.builder()
            .propertyName("mailSubject")
            .usageDataType("java.lang.String")
            .build();
        ComponentProperty componentProperty = new ComponentProperty(stringMeta, "[]");
        assertFalse(componentProperty.valueNotSet());
    }
}
