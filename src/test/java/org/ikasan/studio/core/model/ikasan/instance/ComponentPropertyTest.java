package org.ikasan.studio.core.model.ikasan.instance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentPropertyTest {

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
}
