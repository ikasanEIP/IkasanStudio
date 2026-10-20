package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NonNls;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
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

    @NonNls private static final String REQUIRED_VALUE_FRAGMENT = "must be set";
    @NonNls private static final String EMAIL_EXAMPLE = "name@example.com";

    private static ComponentMeta emailProducerMeta;

    @BeforeAll
    public static void loadEmailProducerMeta() throws Exception {
        emailProducerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer");
    }

    private static FlowElement newEmailProducer() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement,
                                                     Map<String, ComponentPropertyEditRow> sharedRowMap) {
        String propertyName = "toRecipient";
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, sharedRowMap);
    }

    @Test
    public void aLowercaseEmailAddressIsAccepted() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        // A genuine user keystroke, not a pre-existing model value - matches the reported real-world address.
        toRecipientRow.getOverridingInputField().setText("davidhilton68t@gmail.com");

        assertTrue(toRecipientRow.doValidateAll().isEmpty(), "an ordinary lowercase email address must pass validation");
    }

    @Test
    public void aMalformedAddressIsStillRejected() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        toRecipientRow.getOverridingInputField().setText("not-an-email-address");

        assertFalse(toRecipientRow.doValidateAll().isEmpty(), "a genuinely malformed address must still be rejected");
    }

    @Test
    public void theValidationMessageNamesTheOffendingProperty() {
        FlowElement flowElement = newEmailProducer();
        ComponentPropertyEditRow toRecipientRow = rowFor(flowElement, new HashMap<>());
        toRecipientRow.resetDataEntryComponentsWithNewValues();

        toRecipientRow.getOverridingInputField().setText("not-an-email-address");

        List<ValidationInfo> issues = toRecipientRow.doValidateAll();
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).message.startsWith("toRecipient:"),
                "the validation message must name the property it refers to, was: " + issues.get(0).message);
    }
    @Test
    public void trustedPackagesCanBeClearedButInvalidPackagesAreRejected() throws Exception {
        for (String pack : List.of("V3.3.9", "V4.1.6")) {
            for (String role : List.of("Consumer", "Producer")) {
                var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Spring JMS " + role)
                        .getMetadata("trustedObjectPackages");
                for (String empty : List.of("", "   ")) {
                    var row = new ComponentPropertyEditRow(null,
                            new ComponentProperty(meta, "org.example.cat.domain"), false, () -> {}, new HashMap<>());
                    row.resetDataEntryComponentsWithNewValues();
                    row.getOverridingInputField().setText("*");
                    assertFalse(row.doValidateAll().isEmpty(), "wildcards must still be rejected");
                    row.getOverridingInputField().setText("org.example.domain, org.example.shared");
                    assertTrue(row.doValidateAll().isEmpty());
                    row.getOverridingInputField().setText(empty);
                    assertTrue(row.propertyValueHasChanged());
                    assertTrue(row.doValidateAll().isEmpty(), pack + " " + role + " must allow clearing trust");
                    row.updateValueObjectWithEnteredValues();
                    assertTrue(row.inputfieldIsUnset(), "saving must remove the previous packages");
                }
            }
        }
    }

    @Test
    public void clearingARequiredValueStillFailsValidation() {
        var meta = org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.builder()
                .propertyName("requiredValue").mandatory(true).validation("[a-z]+").build();
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(meta, "old"),
                false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        row.getOverridingInputField().setText("");
        assertTrue(row.doValidateAll().stream().anyMatch(issue -> issue.message.contains(REQUIRED_VALUE_FRAGMENT)));
    }

    @Test
    public void savedInvalidEmailAddressesAreMarkedAndClearWhenCorrectedInBothPacks() throws Exception {
        for (String pack : List.of("V3.3.9", "V4.1.6")) {
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer");
            for (String field : List.of("toRecipient", "from", "ccRecipient", "bccRecipient")) {
                for (String address : List.of("bar.@bar.com", ".bar@bar.com", "bar..x@bar.com",
                        "bar@-example.com", "bar@example-.com", "bar@example..com")) {
                    var row = new ComponentPropertyEditRow(null,
                            new ComponentProperty(meta.getMetadata(field), address), false, () -> {}, new HashMap<>());
                    assertFalse(row.propertyValueHasChanged(), "Reopening the saved value is not an edit");
                    var issues = row.doValidateAll();
                    assertFalse(issues.isEmpty(), pack + " " + field + ": " + address);
                    assertTrue(issues.get(0).message.startsWith(field + ":"));
                    assertTrue(issues.get(0).message.contains(EMAIL_EXAMPLE));
                    org.junit.jupiter.api.Assertions.assertEquals("error",
                            row.getOverridingInputField().getClientProperty("JComponent.outline"));
                    org.junit.jupiter.api.Assertions.assertNotNull(row.getPropertyTitleField().getIcon());
                    row.getOverridingInputField().setText("bar@example.com");
                    assertTrue(row.doValidateAll().isEmpty());
                    org.junit.jupiter.api.Assertions.assertNull(row.getOverridingInputField().getClientProperty("JComponent.outline"));
                    org.junit.jupiter.api.Assertions.assertNull(row.getPropertyTitleField().getIcon());
                }
                for (String address : List.of("First.Last+orders@example.co.uk", "user_name@example-domain.com")) {
                    var row = new ComponentPropertyEditRow(null,
                            new ComponentProperty(meta.getMetadata(field), address), false, () -> {}, new HashMap<>());
                    assertTrue(row.doValidateAll().isEmpty(), address);
                }
            }
        }
    }

    @Test
    public void recipientListsIdentifyTheInvalidEntryUsingTheSameRuleAsTheCollectionDialog() throws Exception {
        for (String pack : List.of("V3.3.9", "V4.1.6")) {
            var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Email Producer");
            for (String field : List.of("toRecipients", "ccRecipients", "bccRecipients")) {
                var property = meta.getMetadata(field);
                var entries = List.of("valid@example.com", "bar.@bar.com");
                var row = new ComponentPropertyEditRow(null,
                        new ComponentProperty(property, entries), false, () -> {}, new HashMap<>());
                assertFalse(row.propertyValueHasChanged());
                var issues = row.doValidateAll();
                assertFalse(issues.isEmpty());
                assertTrue(issues.get(0).message.contains(field));
                assertTrue(issues.get(0).message.contains("2"), issues.get(0).message);
                org.junit.jupiter.api.Assertions.assertEquals(
                        ComponentPropertyEditRow.patternValidationMessage(property, entries), issues.get(0).message);
                org.junit.jupiter.api.Assertions.assertNull(ComponentPropertyEditRow.patternValidationMessage(
                        property, List.of("first.last@example.com", "second@example.com")));
                org.junit.jupiter.api.Assertions.assertNotNull(ComponentPropertyEditRow.patternValidationMessage(
                        property, List.of("")));
            }
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void routerListsValidateTheWholeListAndRetainRouteOrder(String pack) throws Exception {
        var single = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Single Recipient Router").getMetadata("routeNames");
        var row = new ComponentPropertyEditRow(null, new ComponentProperty(single), false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        for (String text : List.of("odd,even", "odd, even")) {
            row.getOverridingInputField().setText(text);
            org.junit.jupiter.api.Assertions.assertEquals(List.of("odd", "even"), row.getValue());
            assertTrue(row.doValidateAll().isEmpty(), text);
        }
        for (String text : List.of("odd", "odd,", "odd,,even", "odd,bad-name")) {
            row.getOverridingInputField().setText(text);
            assertFalse(row.doValidateAll().isEmpty(), text);
        }
        org.junit.jupiter.api.Assertions.assertNotNull(ComponentPropertyEditRow.patternValidationMessage(single, List.of("odd", "odd")));
        org.junit.jupiter.api.Assertions.assertNull(ComponentPropertyEditRow.patternValidationMessage(single, List.of("odd", "even", "odd")));
        var multi = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Multi Recipient Router").getMetadata("routeNames");
        org.junit.jupiter.api.Assertions.assertNull(ComponentPropertyEditRow.patternValidationMessage(multi, List.of("odd")));
        org.junit.jupiter.api.Assertions.assertNotNull(ComponentPropertyEditRow.patternValidationMessage(multi, List.of("odd", "bad-name")));
    }

}
