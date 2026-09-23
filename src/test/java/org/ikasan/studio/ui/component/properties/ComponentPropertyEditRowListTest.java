package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComponentPropertyEditRowListTest {
    private ComponentPropertyEditRow row(Object value) {
        var meta = ComponentPropertyMeta.builder().propertyName("filenames")
                .usageDataType("java.util.List<String>").build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, value),
                false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        return row;
    }

    @Test
    void legacyFilenameBracketsAreVisibleWithoutChangingTheSavedValue() {
        var row = row("[myFile\\.txt, anotherFile\\.txt]");
        assertEquals("[myFile\\.txt, anotherFile\\.txt]", row.getOverridingInputField().getText());
        assertEquals(List.of("[myFile\\.txt", "anotherFile\\.txt]"), row.getValue());
        assertFalse(row.propertyValueHasChanged());
    }

    @Test
    void editsRevertsAndClearingAreDetectedBeforeFocusCommit() {
        var row = row("[myFile\\.txt, anotherFile\\.txt]");
        row.getOverridingInputField().setText("changed\\.txt");
        assertTrue(row.propertyValueHasChanged());
        assertEquals(List.of("changed\\.txt"), row.getValue());
        row.getOverridingInputField().setText("myFile\\.txt,anotherFile\\.txt");
        assertTrue(row.propertyValueHasChanged(), "Removing the hidden legacy wrapper must enable Update Code");
        assertEquals("myFile\\.txt,anotherFile\\.txt", row.updateValueObjectWithEnteredValues().getValueString());
        row.getOverridingInputField().setText("[myFile\\.txt, anotherFile\\.txt]");
        assertFalse(row.propertyValueHasChanged());
        row.getOverridingInputField().setText("");
        assertTrue(row.propertyValueHasChanged());
        assertNull(row.getValue());
    }

    @Test
    void validRegexCharacterClassesSurviveDisplayAndSave() {
        String pattern = "[a-z]+[.]txt";
        for (Object value : List.of(pattern, List.of(pattern))) {
            var row = row(value);
            assertEquals(pattern, row.getOverridingInputField().getText());
            assertFalse(row.propertyValueHasChanged());
            String saved = row.updateValueObjectWithEnteredValues().getValueString();
            assertEquals(pattern, saved);
            assertTrue(java.util.regex.Pattern.compile(saved).matcher("invoice.txt").matches());
            assertFalse(java.util.regex.Pattern.compile(saved).matcher("123.txt").matches());
        }
    }

    @Test
    void nativeListsAlsoRemainUnchanged() {
        assertFalse(row(List.of("first", "second")).propertyValueHasChanged());
    }
}
