package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test: EmailProducerBuilderImpl#validate() (Ikasan core) throws
 * "Email must have at least 1 recipient of 'to', 'cc', or 'bcc'" at Spring Boot startup if none of
 * toRecipient(s)/ccRecipient(s)/bccRecipient(s) are set - previously undetectable in the properties panel, only
 * surfacing as a runtime ApplicationContext failure. toRecipient/toRecipients/ccRecipient/ccRecipients/
 * bccRecipient/bccRecipients are now each declared mandatoryUnlessAnyOf the other five, exactly like SFTP
 * Consumer's password/privateKeyFilename (see ComponentPropertyEditRowConditionalMandatoryTest) but six-way
 * rather than pairwise: any one of the six being set satisfies all the others.
 */
public class ComponentPropertyEditRowEmailRecipientMandatoryTest {

    private static ComponentMeta emailProducerMeta;

    @BeforeAll
    public static void loadEmailProducerMeta() throws Exception {
        emailProducerMeta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer");
    }

    private static FlowElement newEmailProducer() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        flowElement.setPropertyValue("from", "sender@example.com");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName,
                                                     Map<String, ComponentPropertyEditRow> sharedRowMap) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, sharedRowMap);
    }

    private static Map<String, ComponentPropertyEditRow> allSixRows(FlowElement flowElement) {
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();
        for (String propertyName : new String[]{"toRecipient", "toRecipients", "ccRecipient", "ccRecipients", "bccRecipient", "bccRecipients"}) {
            ComponentPropertyEditRow row = rowFor(flowElement, propertyName, sharedRowMap);
            row.resetDataEntryComponentsWithNewValues();
        }
        return sharedRowMap;
    }

    @Test
    public void allSixUnsetFailsValidationOnEveryRow() {
        FlowElement flowElement = newEmailProducer();
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);

        for (Map.Entry<String, ComponentPropertyEditRow> entry : rows.entrySet()) {
            assertFalse(entry.getValue().doValidateAll().isEmpty(), entry.getKey() + " must be flagged - no recipient is set anywhere");
        }
    }

    @Test
    public void settingToRecipientSatisfiesAllFiveOthers() {
        FlowElement flowElement = newEmailProducer();
        flowElement.setPropertyValue("toRecipient", "someone@example.com");
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);

        for (Map.Entry<String, ComponentPropertyEditRow> entry : rows.entrySet()) {
            assertTrue(entry.getValue().doValidateAll().isEmpty(), entry.getKey() + " should not be required - toRecipient already supplies a recipient");
        }
    }

    @Test
    public void settingToRecipientsListSatisfiesAllFiveOthers() {
        FlowElement flowElement = newEmailProducer();
        flowElement.setPropertyValue("toRecipients", "someone@example.com,someoneelse@example.com");
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);

        for (Map.Entry<String, ComponentPropertyEditRow> entry : rows.entrySet()) {
            assertTrue(entry.getValue().doValidateAll().isEmpty(), entry.getKey() + " should not be required - toRecipients already supplies a recipient");
        }
    }

    @Test
    public void settingBccRecipientSatisfiesAllFiveOthers() {
        // Deliberately the "last" field alphabetically/positionally, to prove satisfaction isn't order-dependent.
        FlowElement flowElement = newEmailProducer();
        flowElement.setPropertyValue("bccRecipient", "hidden@example.com");
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);

        for (Map.Entry<String, ComponentPropertyEditRow> entry : rows.entrySet()) {
            assertTrue(entry.getValue().doValidateAll().isEmpty(), entry.getKey() + " should not be required - bccRecipient already supplies a recipient");
        }
    }

    @Test
    public void labelsCarryNoPerFieldEitherOrCue() {
        // Unlike SFTP's pairwise password/privateKeyFilename (see ComponentPropertyEditRowConditionalMandatoryTest),
        // a five-way "(or a / b / c / d / e)" suffix on every single row is unreadable - these six instead share
        // a single mandatorySectionHeading ("At least one of..."), which suppresses the per-field bracket cue.
        FlowElement flowElement = newEmailProducer();
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);

        // No displayLabel is set for these fields, so the label falls back to the raw propertyName.
        assertEquals("toRecipient", rows.get("toRecipient").getPropertyTitleField().getText());
        assertEquals("bccRecipient", rows.get("bccRecipient").getPropertyTitleField().getText());
        for (Map.Entry<String, ComponentPropertyEditRow> entry : rows.entrySet()) {
            assertFalse(entry.getValue().getPropertyTitleField().getText().contains("(or "),
                    entry.getKey() + "'s label must not carry the per-field (or ...) cue");
        }
    }

    @Test
    public void livelyTypingIntoCcRecipientUnblocksToRecipient() {
        FlowElement flowElement = newEmailProducer();
        Map<String, ComponentPropertyEditRow> rows = allSixRows(flowElement);
        ComponentPropertyEditRow toRecipientRow = rows.get("toRecipient");
        ComponentPropertyEditRow ccRecipientRow = rows.get("ccRecipient");
        assertFalse(toRecipientRow.doValidateAll().isEmpty(), "sanity check - starts out unsatisfied");

        // A genuine user keystroke into a sibling field, not a pre-existing model value.
        ccRecipientRow.getOverridingInputField().setText("cc@example.com");

        assertTrue(toRecipientRow.doValidateAll().isEmpty(), "typing a CC recipient live should immediately unblock toRecipient's validation");
    }
}
