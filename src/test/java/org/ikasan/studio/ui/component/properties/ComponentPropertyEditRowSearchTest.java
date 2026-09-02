package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Large components (SftpConsumer, SpringJmsConsumer) can have 20-30+ properties spread across several collapsed
 * optional groups, making a half-remembered property hard to find. Verifies the row-level matching that backs the
 * properties-panel search field: ComponentPropertyEditRow#matchesSearch() (name/display-label/help-text, case
 * insensitive) and #setRowVisible() (used to collapse non-matching rows' GridBagLayout space).
 */
public class ComponentPropertyEditRowSearchTest {

    // Loaded once for the whole class - repeatedly opening the metapack filesystem per test trips the test
    // framework's thread-leak detector.
    private static ComponentMeta ftpConsumerMeta;

    @BeforeAll
    public static void loadFtpConsumerMeta() throws Exception {
        ftpConsumerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "FTP Consumer");
    }

    private static ComponentPropertyEditRow rowFor(String propertyName) {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(ftpConsumerMeta)
                .componentName("My FTP Consumer")
                .build();
        ComponentProperty property = new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, null);
    }

    @Test
    public void blankQueryMatchesEverything() {
        ComponentPropertyEditRow row = rowFor("remoteHost");
        assertTrue(row.matchesSearch(""), "empty query should match");
        assertTrue(row.matchesSearch(null), "null query should match");
        assertTrue(row.matchesSearch("   "), "whitespace-only query should match");
    }

    @Test
    public void matchesOnPropertyNameCaseInsensitively() {
        ComponentPropertyEditRow row = rowFor("remoteHost");
        assertTrue(row.matchesSearch("remotehost"), "should match the property name regardless of case");
        assertTrue(row.matchesSearch("HOST"), "should match a substring of the property name regardless of case");
    }

    @Test
    public void matchesOnHelpTextEvenWhenNameDoesNotMatch() {
        // ftpsKeyStoreFilePath's help text mentions "keystore" - a user who remembers what the field does but
        // not its exact camelCase name should still be able to find it.
        ComponentPropertyEditRow row = rowFor("ftpsKeyStoreFilePath");
        assertTrue(row.matchesSearch("keystore"), "should match a term that only appears in the help text");
    }

    @Test
    public void doesNotMatchUnrelatedTerm() {
        ComponentPropertyEditRow row = rowFor("remoteHost");
        assertFalse(row.matchesSearch("keystore"), "should not match a term absent from name, label, and help text");
    }

    @Test
    public void setRowVisibleTogglesTheLabelAndInputComponents() {
        ComponentPropertyEditRow row = rowFor("remoteHost");
        assertTrue(row.getPropertyTitleField().isVisible(), "row starts visible");

        row.setRowVisible(false);
        assertFalse(row.getPropertyTitleField().isVisible(), "label should be hidden");
        assertFalse(row.getInputField().getFirstFocusComponent().isVisible(), "input should be hidden");

        row.setRowVisible(true);
        assertTrue(row.getPropertyTitleField().isVisible(), "label should be visible again");
        assertTrue(row.getInputField().getFirstFocusComponent().isVisible(), "input should be visible again");
    }
}
