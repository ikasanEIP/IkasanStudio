package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class StringCollectionEditRowTest {
    @Test void preservesValuesAndDistinguishesEmptyFromUnset() {
        for (Class<?> type : List.of(List.class, Map.class)) {
            Object value = type == List.class ? List.of("a,b", "a,b", "\n\\\"") : Map.of("key", "a,b\n\\\"");
            var meta = ComponentPropertyMeta.builder().propertyName("entries").propertyDataType(type).build();
            var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, value), false);
            row.resetDataEntryComponentsWithNewValues();
            assertThat(row.getValue()).isEqualTo(value);
            assertThat(row.getOverridingInputField().isEditable()).isFalse();
            row.getOverridingInputField().setText(type == List.class ? "[]" : "{}");
            assertThat(row.inputfieldIsUnset()).isFalse();
            row.clearValue();
            assertThat(row.inputfieldIsUnset()).isTrue();
        }
    }
    @Test void explicitEmptyDoesNotSatisfyRequiredEntries() {
        var meta = ComponentPropertyMeta.builder().propertyName("entries").propertyDataType(List.class).mandatory(true).build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, List.of()), false);
        row.resetDataEntryComponentsWithNewValues();
        assertThat(row.inputfieldIsUnset()).isFalse();
        assertThat(row.doValidateAll()).isNotEmpty();
    }
    @Test void legacyInvalidMapRemainsVisibleAndReportsValidationError() {
        var meta = ComponentPropertyMeta.builder().propertyName("entries").propertyDataType(Map.class).build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, "old Java expression"), false);
        row.resetDataEntryComponentsWithNewValues();
        assertThat(row.getValue()).isEqualTo("old Java expression");
        assertThat(row.doValidateAll()).isNotEmpty();
    }
}
