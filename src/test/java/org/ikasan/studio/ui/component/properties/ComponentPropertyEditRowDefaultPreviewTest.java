package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A never-set property whose meta declares a defaultValue is always previewed in the UI (booleans as a
 * pre-selected checkbox, everything else as pre-filled text) so it can be reviewed without any button click,
 * but must never be silently treated as if the user had entered it. Only a genuine edit - typing, ticking a
 * checkbox, picking a choice - should cause the value to be committed and, in turn, written into generated code.
 */
public class ComponentPropertyEditRowDefaultPreviewTest {

    // Loaded once for the whole class - repeatedly opening the metapack filesystem per test trips the test
    // framework's thread-leak detector.
    private static ComponentMeta ftpConsumerMeta;

    @BeforeAll
    public static void loadFtpConsumerMeta() throws Exception {
        ftpConsumerMeta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(TestFixtures.BASE_META_PACK, "FTP Consumer");
    }

    private static FlowElement newUnpopulatedFtpConsumer() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(ftpConsumerMeta)
                .componentName("My FTP Consumer")
                .build();
        flowElement.setPropertyValue("cronExpression", "*/5 * * * * ?");
        flowElement.setPropertyValue("filenamePattern", "*Test.txt");
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("remoteHost", "myRemortHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("sourceDirectory", "/test/source/directory/");
        flowElement.setPropertyValue("username", "myLoginName");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        // Real usage (ComponentPropertiesPanel) always supplies a change listener - that's what wires up the
        // Document/Action/Item listeners that clear showingDefaultOnly on a genuine edit, so tests must too.
        return new ComponentPropertyEditRow(null, property, false, () -> { }, null);
    }

    @Test
    public void neverSetLongPropertyStaysUnsetEvenWhenPanelRefreshes() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "minAge");
        row.resetDataEntryComponentsWithNewValues();
        assertNull(row.getValue(), "minAge must stay unset when never touched");
        assertFalse(row.propertyValueHasChanged());
    }

    @Test
    public void neverSetLongPropertyPreviewsItsDefaultTextWithoutAnyButtonClick() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "minAge");
        row.resetDataEntryComponentsWithNewValues();
        assertEquals("120", row.getOverridingInputField().getText(), "the field should show minAge's default (120) for review");
        assertNull(row.getValue(), "showing the default must not count as the user having set it");
        assertFalse(row.propertyValueHasChanged());
        assertNull(row.getComponentProperty().getValue(), "the underlying model value must remain untouched");
    }

    @Test
    public void neverSetBooleanPropertyStaysUnsetDespiteShowingItsDefaultCheckbox() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "criticalOnStartup");
        row.resetDataEntryComponentsWithNewValues();
        assertNull(row.getValue(), "criticalOnStartup must stay unset - the checkbox is only previewing its default");
        assertFalse(row.propertyValueHasChanged());
    }

    @Test
    public void clickingTheCheckboxGenuinelyCommitsIt() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "criticalOnStartup");
        row.resetDataEntryComponentsWithNewValues();
        // criticalOnStartup defaults to true, so the "true" box already shows selected as a preview; click the
        // "false" box instead to make a genuine, unambiguous choice that differs from the previewed default.
        row.getInputField().getFalseBox().doClick();
        assertEquals(Boolean.FALSE, row.getValue());
        assertTrue(row.propertyValueHasChanged());
    }

    @Test
    public void setDefaultsPreviewsWithoutCommitting() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "minAge");
        row.resetDataEntryComponentsWithNewValues();
        row.setDefaultValue();
        assertNull(row.getValue(), "Set Defaults must only preview, not commit");
        assertFalse(row.propertyValueHasChanged());
        assertNull(row.getComponentProperty().getValue(), "the underlying model value must remain untouched");
    }

    @Test
    public void editingAfterSetDefaultsGenuinelyCommitsIt() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "minAge");
        row.resetDataEntryComponentsWithNewValues();
        row.setDefaultValue();
        // .setValue() (not .setText()) mirrors what happens when a real user's edit is committed by the
        // JFormattedTextField's formatter (e.g. on focus-lost, such as clicking "Update Code").
        row.getOverridingInputField().setValue(60);
        assertEquals(60, row.getValue());
        assertTrue(row.propertyValueHasChanged());
    }

    @Test
    public void setDefaultsSkipsSubstitutionPlaceholders() {
        ComponentPropertyEditRow row = rowFor(newUnpopulatedFtpConsumer(), "configurationId");
        row.resetDataEntryComponentsWithNewValues();
        row.setDefaultValue();
        assertFalse(row.propertyValueHasChanged(), "a \"__...\" substitution default must not be previewed as a literal string");
    }
}
