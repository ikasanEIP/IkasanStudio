package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ComponentPropertyPackageChooserTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void appendsPackagesWithoutDuplicatesAndAllowsManualEditingAndClearing(String pack) throws Exception {
        for (String role : new String[]{"Consumer", "Producer"}) {
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Spring JMS " + role)
                    .getMetadata("trustedObjectPackages");
            var edits = new AtomicInteger();
            var property = new ComponentProperty(meta, "org.example.orders");
            var row = new ComponentPropertyEditRow(null, property, false, edits::incrementAndGet, new HashMap<>());
            row.resetDataEntryComponentsWithNewValues();
            assertNotNull(row.getChooseValueButton());
            assertEquals("Add package...", row.getChooseValueButton().getText());
            row.appendSelectedPackage(null);
            row.appendSelectedPackage("");
            row.appendSelectedPackage("org.example.orders");
            assertFalse(row.propertyValueHasChanged(), "cancel, default package and duplicates leave the field alone");
            int before = edits.get();
            row.appendSelectedPackage("org.example.shared");
            assertTrue(edits.get() > before, "selection must notify the unsaved-edit listener");
            assertEquals("org.example.orders, org.example.shared", row.getOverridingInputField().getText());
            row.appendSelectedPackage("org.example.shared");
            assertEquals("org.example.orders, org.example.shared", row.getOverridingInputField().getText());
            assertTrue(row.doValidateAll().isEmpty());
            row.updateValueObjectWithEnteredValues();
            assertEquals("org.example.orders, org.example.shared", property.getValue());
            row.getOverridingInputField().setText("org.external.payloads,");
            row.appendSelectedPackage("org.example.shared");
            assertEquals("org.external.payloads, org.example.shared", row.getOverridingInputField().getText());
            row.getOverridingInputField().setText("");
            assertTrue(row.doValidateAll().isEmpty());
            row.updateValueObjectWithEnteredValues();
            assertTrue(row.inputfieldIsUnset());
            row.appendSelectedPackage("org.example.orders");
            assertEquals("org.example.orders", row.getOverridingInputField().getText());
        }
    }
}
