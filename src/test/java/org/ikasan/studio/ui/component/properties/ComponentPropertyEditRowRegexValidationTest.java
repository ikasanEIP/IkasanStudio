package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for Email Producer's toRecipient/from/ccRecipient/bccRecipient email-address validation:
 * the original regex ("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$", compiled with no CASE_INSENSITIVE flag) only
 * ever matched an ALL-UPPERCASE address, rejecting every ordinary lowercase email a real user would enter (e.g.
 * "davidhilton68t@gmail.com"). Also covers the reported validation dialog giving no indication of which field
 * it refers to - {@link ComponentPropertyEditRow#doValidateAll()} now prefixes the property name onto the regex
 * validation message, matching the mandatory/mandatoryUnlessAnyOf/mandatoryIfTrue branches just above it.
 */
public class ComponentPropertyEditRowRegexValidationTest {

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

    @Test
    public void aLowercaseEmailAddressIsAccepted() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, "toRecipient", new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        // A genuine user keystroke, not a pre-existing model value - matches the reported real-world address.
        toRecipientRow.getOverridingInputField().setText("davidhilton68t@gmail.com");

        assertTrue(toRecipientRow.doValidateAll().isEmpty(), "an ordinary lowercase email address must pass validation");
    }

    @Test
    public void aMalformedAddressIsStillRejected() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, "toRecipient", new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        toRecipientRow.getOverridingInputField().setText("not-an-email-address");

        assertFalse(toRecipientRow.doValidateAll().isEmpty(), "a genuinely malformed address must still be rejected");
    }

    @Test
    public void theValidationMessageNamesTheOffendingProperty() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, "toRecipient", new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        toRecipientRow.getOverridingInputField().setText("not-an-email-address");

        List<ValidationInfo> issues = toRecipientRow.doValidateAll();
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).message.startsWith("toRecipient:"),
                "the validation message must name the property it refers to, was: " + issues.get(0).message);
    }
}
