package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FTP Consumer's ftpsKeyStoreFilePath/ftpsKeyStoreFilePassword are each declared mandatoryIfTrue "ftps" - the
 * underlying connector (FileTransferProtocolSSLClient#doConnect() in both ik3 and ik4) unconditionally reads both
 * once ftps is enabled, with no fallback to a default Java keystore, so leaving either blank while ftps is true
 * fails at runtime with no help from the UI. Verifies that ComponentPropertyEditRow#doValidateAll() enforces this
 * by consulting the "ftps" sibling row's live value via the shared componentPropertyEditBoxMap.
 */
public class ComponentPropertyEditRowMandatoryIfTrueTest {

    // Loaded once for the whole class - repeatedly opening the metapack filesystem per test trips the test
    // framework's thread-leak detector.
    private static ComponentMeta ftpConsumerMeta;

    @BeforeAll
    public static void loadFtpConsumerMeta() throws Exception {
        ftpConsumerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "FTP Consumer");
    }

    private static FlowElement newFtpConsumer() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(ftpConsumerMeta)
                .componentName("My FTP Consumer")
                .build();
        flowElement.setPropertyValue("cronExpression", "*/5 * * * * ?");
        flowElement.setPropertyValue("filenamePattern", "*Test.txt");
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("remoteHost", "myRemoteHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("sourceDirectory", "/test/source/directory/");
        flowElement.setPropertyValue("username", "myLoginName");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName,
                                                     Map<String, ComponentPropertyEditRow> sharedRowMap) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        // Real usage (ComponentPropertiesPanel) always supplies a change listener and a shared map - that's what
        // wires up the cross-row sibling lookup mandatoryIfTrue depends on, so tests must too.
        return new ComponentPropertyEditRow(null, property, false, () -> { }, sharedRowMap);
    }

    @Test
    public void keyStorePathNotRequiredWhileFtpsIsUnset() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newFtpConsumer();
        ComponentPropertyEditRow ftpsRow = rowFor(flowElement, "ftps", sharedRowMap);
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", sharedRowMap);
        ftpsRow.resetDataEntryComponentsWithNewValues();
        keyStorePathRow.resetDataEntryComponentsWithNewValues();

        assertTrue(keyStorePathRow.doValidateAll().isEmpty(), "ftpsKeyStoreFilePath should not be required while ftps is unset (previewing its false default)");
    }

    @Test
    public void keyStorePathAndPasswordRequiredOnceFtpsIsSet() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newFtpConsumer();
        flowElement.setPropertyValue("ftps", true);
        ComponentPropertyEditRow ftpsRow = rowFor(flowElement, "ftps", sharedRowMap);
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", sharedRowMap);
        ComponentPropertyEditRow keyStorePasswordRow = rowFor(flowElement, "ftpsKeyStoreFilePassword", sharedRowMap);
        ftpsRow.resetDataEntryComponentsWithNewValues();
        keyStorePathRow.resetDataEntryComponentsWithNewValues();
        keyStorePasswordRow.resetDataEntryComponentsWithNewValues();

        assertFalse(keyStorePathRow.doValidateAll().isEmpty(), "ftpsKeyStoreFilePath must be flagged once ftps is enabled - the connector has no fallback keystore");
        assertFalse(keyStorePasswordRow.doValidateAll().isEmpty(), "ftpsKeyStoreFilePassword must be flagged once ftps is enabled - the connector has no fallback keystore");
    }

    @Test
    public void keyStorePathSatisfiedOnceAValueIsSuppliedWithFtpsOn() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newFtpConsumer();
        flowElement.setPropertyValue("ftps", true);
        flowElement.setPropertyValue("ftpsKeyStoreFilePath", "/etc/keystores/ftps.jks");
        ComponentPropertyEditRow ftpsRow = rowFor(flowElement, "ftps", sharedRowMap);
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", sharedRowMap);
        ftpsRow.resetDataEntryComponentsWithNewValues();
        keyStorePathRow.resetDataEntryComponentsWithNewValues();

        assertTrue(keyStorePathRow.doValidateAll().isEmpty(), "ftpsKeyStoreFilePath should be satisfied once a value is supplied");
    }

    @Test
    public void livelyClickingFtpsRequiresKeyStorePathImmediately() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newFtpConsumer();
        ComponentPropertyEditRow ftpsRow = rowFor(flowElement, "ftps", sharedRowMap);
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", sharedRowMap);
        ftpsRow.resetDataEntryComponentsWithNewValues();
        keyStorePathRow.resetDataEntryComponentsWithNewValues();
        assertTrue(keyStorePathRow.doValidateAll().isEmpty(), "sanity check - starts out unrequired");

        // A genuine user click on the "true" checkbox, not a pre-existing model value.
        ftpsRow.getInputField().getTrueBox().doClick();

        assertFalse(keyStorePathRow.doValidateAll().isEmpty(), "ticking ftps live should immediately require ftpsKeyStoreFilePath");
    }

    @Test
    public void labelCarriesTheRequiredIfEnabledCue() {
        FlowElement flowElement = newFtpConsumer();
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", new HashMap<>());

        assertEquals("ftpsKeyStoreFilePath (required if ftps is enabled)", keyStorePathRow.getPropertyTitleField().getText());
    }

    @Test
    public void notRequiredWhenNoSharedRowMapIsWired() {
        // Defensive: without a shared map (e.g. a row constructed in isolation) there is no "ftps" sibling to
        // consult, so the conditional check must not throw and must not report required-by-default.
        FlowElement flowElement = newFtpConsumer();
        flowElement.setPropertyValue("ftps", true);
        ComponentPropertyEditRow keyStorePathRow = rowFor(flowElement, "ftpsKeyStoreFilePath", null);
        keyStorePathRow.resetDataEntryComponentsWithNewValues();

        assertTrue(keyStorePathRow.doValidateAll().isEmpty(), "with no sibling to consult, ftpsKeyStoreFilePath must not be reported as required");
    }
}
