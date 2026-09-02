package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression test: a List&lt;String&gt; ("STRING_LIST") property left empty in its text field used to report a
 * value of [""] (a one-element list holding a single blank string) rather than "unset" - "".split(",") still
 * yields a single blank element. Downstream, componentFactory_en.ftl only skips emitting a setter call for a
 * genuinely null property value, so an emptied Email Producer toRecipients field generated the uncompilable
 * ".setToRecipients()" - Toggling into the field and leaving it blank must now report as unset, matching every
 * other property type in {@link ComponentPropertyEditRow#getValue()}.
 */
public class ComponentPropertyEditRowStringListTest {

    private static ComponentMeta emailProducerMeta;

    @BeforeAll
    public static void loadEmailProducerMeta() throws Exception {
        emailProducerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer");
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, new HashMap<>());
    }

    @Test
    public void anEmptiedListFieldReportsAsUnsetNotASingleBlankElement() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        ComponentPropertyEditRow toRecipientsRow = rowFor(flowElement, "toRecipients");
        toRecipientsRow.resetDataEntryComponentsWithNewValues();

        // A genuine user keystroke into a field, then clearing it back out - not a pre-existing model value.
        toRecipientsRow.getOverridingInputField().setText("someone@example.com");
        toRecipientsRow.getOverridingInputField().setText("");

        assertNull(toRecipientsRow.getValue(), "an emptied list field must report as unset, not [\"\"]");
    }

    @Test
    public void aNeverTouchedListFieldStaysUnset() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        ComponentPropertyEditRow toRecipientsRow = rowFor(flowElement, "toRecipients");
        toRecipientsRow.resetDataEntryComponentsWithNewValues();

        assertNull(toRecipientsRow.getValue());
    }

    @Test
    public void aGenuinelyPopulatedListFieldStillParsesCorrectly() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        ComponentPropertyEditRow toRecipientsRow = rowFor(flowElement, "toRecipients");
        toRecipientsRow.resetDataEntryComponentsWithNewValues();

        toRecipientsRow.getOverridingInputField().setText("one@example.com,two@example.com");

        assertEquals(List.of("one@example.com", "two@example.com"), toRecipientsRow.getValue());
    }
}
