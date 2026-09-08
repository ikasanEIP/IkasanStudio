package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class ComponentPropertyEditRowWhitespaceTest {
    private ComponentPropertyEditRow row(boolean preserve, String initial) {
        var meta = ComponentPropertyMeta.builder().propertyName("test")
                .preserveWhitespace(preserve).validation(preserve ? "" : "[a-z]+").build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, initial),
                false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        return row;
    }

    @Test
    void validatesTrimmedValueWithoutChangingTypingAndDisplaysCommittedValue() {
        var row = row(false, "old");
        row.getOverridingInputField().setText("  pasted \t");
        assertTrue(row.doValidateAll().isEmpty());
        assertEquals("  pasted \t", row.getOverridingInputField().getText());
        assertEquals("pasted", row.updateValueObjectWithEnteredValues().getValue());
        assertEquals("pasted", row.getOverridingInputField().getText());
    }

    @Test
    void preservesLiteralAndWhitespaceOnlyReplacement() {
        var row = row(true, "old");
        row.getOverridingInputField().setText(" ");
        assertEquals(" ", row.updateValueObjectWithEnteredValues().getValue());
        assertEquals(" ", row.getOverridingInputField().getText());
    }

    @Test
    void trimsLiveEditableDropdownBeforeFocusCommit() {
        var meta = ComponentPropertyMeta.builder().propertyName("format")
                .choices(java.util.List.of("text/plain")).choicesEditable(true).build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, "text/plain"),
                false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        row.getInputField().getPropertyChoiceValueField().getEditor().setItem(" text/html ");
        assertEquals("text/html", row.updateValueObjectWithEnteredValues().getValue());
        assertEquals("text/html", row.getInputField().getPropertyChoiceValueField().getEditor().getItem());
    }

    @Test
    void bundledMetadataPreservesSensitiveFields() throws Exception {
        for (String pack : java.util.List.of("V3.3.9", "V4.1.6")) {
            var logging = org.ikasan.studio.core.metapack.ComponentLibrary
                    .getIkasanComponentByKeyMandatory(pack, "Logging Producer");
            assertTrue(logging.getMetadata("replacementText").isPreserveWhitespace());
            assertTrue(logging.getMetadata("regExpPattern").isPreserveWhitespace());
            assertFalse(logging.getMetadata("componentName").isPreserveWhitespace());
            var email = org.ikasan.studio.core.metapack.ComponentLibrary
                    .getIkasanComponentByKeyMandatory(pack, "Email Producer");
            assertTrue(email.getMetadata("mailPassword").isPreserveWhitespace());
            assertTrue(email.getMetadata("emailBody").isPreserveWhitespace());
        }
    }

    @Test
    void leavesUntouchedExistingValuesAlone() {
        var row = row(false, " existing ");
        assertFalse(row.propertyValueHasChanged());
        assertEquals(" existing ", row.updateValueObjectWithEnteredValues().getValue());
    }
}
