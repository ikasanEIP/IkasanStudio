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
    void legacyFilenamesDoNotCountAsAnEditOnSelection() {
        var row = row("[myFile\\.txt, anotherFile\\.txt]");
        assertEquals(List.of("myFile\\.txt", "anotherFile\\.txt"), row.getValue());
        assertFalse(row.propertyValueHasChanged());
    }

    @Test
    void editsRevertsAndClearingAreDetectedBeforeFocusCommit() {
        var row = row("[myFile\\.txt, anotherFile\\.txt]");
        row.getOverridingInputField().setText("changed\\.txt");
        assertTrue(row.propertyValueHasChanged());
        assertEquals(List.of("changed\\.txt"), row.getValue());
        row.getOverridingInputField().setText("myFile\\.txt, anotherFile\\.txt");
        assertFalse(row.propertyValueHasChanged());
        row.getOverridingInputField().setText("");
        assertTrue(row.propertyValueHasChanged());
        assertNull(row.getValue());
    }

    @Test
    void nativeListsAlsoRemainUnchanged() {
        assertFalse(row(List.of("first", "second")).propertyValueHasChanged());
    }
}
