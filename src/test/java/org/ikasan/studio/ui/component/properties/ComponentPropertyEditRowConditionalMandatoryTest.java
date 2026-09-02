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
 * SFTP Consumer's password and privateKeyFilename are each declared mandatoryUnlessAnyOf the other - exactly one
 * of the two credential mechanisms must be supplied, but neither is unconditionally required on its own. Verifies
 * that ComponentPropertyEditRow#doValidateAll() enforces this by consulting the sibling row's live value via the
 * shared componentPropertyEditBoxMap, rather than each row validating itself in isolation.
 */
public class ComponentPropertyEditRowConditionalMandatoryTest {

    // Loaded once for the whole class - repeatedly opening the metapack filesystem per test trips the test
    // framework's thread-leak detector.
    private static ComponentMeta sftpConsumerMeta;

    @BeforeAll
    public static void loadSftpConsumerMeta() throws Exception {
        sftpConsumerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "SFTP Consumer");
    }

    private static FlowElement newSftpConsumer() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(sftpConsumerMeta)
                .componentName("My SFTP Consumer")
                .build();
        flowElement.setPropertyValue("cronExpression", "*/5 * * * * ?");
        flowElement.setPropertyValue("filenamePattern", "*Test.txt");
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
        // wires up the cross-row sibling lookup mandatoryUnlessAnyOf depends on, so tests must too.
        return new ComponentPropertyEditRow(null, property, false, () -> { }, sharedRowMap);
    }

    @Test
    public void bothUnsetFailsValidationOnBothSides() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", sharedRowMap);
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", sharedRowMap);
        passwordRow.resetDataEntryComponentsWithNewValues();
        privateKeyRow.resetDataEntryComponentsWithNewValues();

        assertFalse(passwordRow.doValidateAll().isEmpty(), "password must be flagged - neither credential mechanism is set");
        assertFalse(privateKeyRow.doValidateAll().isEmpty(), "privateKeyFilename must be flagged - neither credential mechanism is set");
    }

    @Test
    public void existingPrivateKeyFilenameSatisfiesPassword() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        flowElement.setPropertyValue("privateKeyFilename", "~/.ssh/id_rsa");
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", sharedRowMap);
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", sharedRowMap);
        passwordRow.resetDataEntryComponentsWithNewValues();
        privateKeyRow.resetDataEntryComponentsWithNewValues();

        assertTrue(passwordRow.doValidateAll().isEmpty(), "password should not be required - privateKeyFilename already supplies a credential");
    }

    @Test
    public void existingPasswordSatisfiesPrivateKeyFilename() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        flowElement.setPropertyValue("password", "secret");
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", sharedRowMap);
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", sharedRowMap);
        passwordRow.resetDataEntryComponentsWithNewValues();
        privateKeyRow.resetDataEntryComponentsWithNewValues();

        assertTrue(privateKeyRow.doValidateAll().isEmpty(), "privateKeyFilename should not be required - password already supplies a credential");
    }

    @Test
    public void livelyTypingIntoPrivateKeyFilenameUnblocksPassword() {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", sharedRowMap);
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", sharedRowMap);
        passwordRow.resetDataEntryComponentsWithNewValues();
        privateKeyRow.resetDataEntryComponentsWithNewValues();
        assertFalse(passwordRow.doValidateAll().isEmpty(), "sanity check - starts out unsatisfied");

        // A genuine user keystroke into the sibling field, not a pre-existing model value.
        privateKeyRow.getOverridingInputField().setText("~/.ssh/id_rsa");

        assertTrue(passwordRow.doValidateAll().isEmpty(), "typing a private key path live should immediately unblock password's validation");
    }

    @Test
    public void labelCarriesTheEitherOrCueDerivedFromMandatoryUnlessAnyOf() {
        FlowElement flowElement = newSftpConsumer();
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", new HashMap<>());
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", new HashMap<>());

        assertEquals("Password (or privateKeyFilename)", passwordRow.getPropertyTitleField().getText());
        assertEquals("Private Key Filename (or password)", privateKeyRow.getPropertyTitleField().getText());
    }

    @Test
    public void knownHostFilenameIsFlaggedWhenNeitherCredentialIsSet() {
        // knownHostFilename is only actually read by the JCA connector when password is null (key-based auth) -
        // see SFTPManagedConnection.java:221-239 in ik3/ik4 core - so it shares password's "not yet decided which
        // auth mechanism" unset state here too.
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        ComponentPropertyEditRow knownHostRow = rowFor(flowElement, "knownHostFilename", sharedRowMap);
        rowFor(flowElement, "password", sharedRowMap);
        knownHostRow.resetDataEntryComponentsWithNewValues();

        assertFalse(knownHostRow.doValidateAll().isEmpty(), "knownHostFilename must be flagged - password is unset, so key-based auth (which needs it) is implied");
    }

    @Test
    public void settingPasswordExemptsKnownHostFilename() {
        // Password auth explicitly disables host-key checking (SFTPClient.java's doConnect() sets
        // StrictHostKeyChecking=no on that path) - knownHostFilename genuinely isn't read at all once password
        // is supplied, so it must stop being required the moment password is.
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        flowElement.setPropertyValue("password", "secret");
        ComponentPropertyEditRow knownHostRow = rowFor(flowElement, "knownHostFilename", sharedRowMap);
        rowFor(flowElement, "password", sharedRowMap);
        knownHostRow.resetDataEntryComponentsWithNewValues();

        assertTrue(knownHostRow.doValidateAll().isEmpty(), "knownHostFilename should not be required once password auth is in use");
    }

    @Test
    public void knownHostFilenameAloneDoesNotSatisfyPassword() {
        // The relationship is one-directional: password is only satisfied by privateKeyFilename, not by
        // knownHostFilename - setting knownHostFilename alone must not silently unblock password's own check.
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        FlowElement flowElement = newSftpConsumer();
        flowElement.setPropertyValue("knownHostFilename", "~/.ssh/known_hosts");
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", sharedRowMap);
        ComponentPropertyEditRow privateKeyRow = rowFor(flowElement, "privateKeyFilename", sharedRowMap);
        rowFor(flowElement, "knownHostFilename", sharedRowMap);
        passwordRow.resetDataEntryComponentsWithNewValues();
        privateKeyRow.resetDataEntryComponentsWithNewValues();

        assertFalse(passwordRow.doValidateAll().isEmpty(), "knownHostFilename alone must not satisfy password's requirement");
        assertFalse(privateKeyRow.doValidateAll().isEmpty(), "knownHostFilename alone must not satisfy privateKeyFilename's requirement either");
    }

    @Test
    public void neitherRequiredWhenNoSharedRowMapIsWired() {
        // Defensive: without a shared map (e.g. a row constructed in isolation) there is no sibling to consult,
        // so the conditional check must not throw and must not report satisfied-by-default.
        FlowElement flowElement = newSftpConsumer();
        ComponentPropertyEditRow passwordRow = rowFor(flowElement, "password", null);
        passwordRow.resetDataEntryComponentsWithNewValues();

        assertFalse(passwordRow.doValidateAll().isEmpty(), "with no sibling to consult, password must still be flagged as required");
    }
}
