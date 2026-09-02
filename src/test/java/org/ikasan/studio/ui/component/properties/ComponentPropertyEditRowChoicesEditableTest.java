package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * emailFormat is a formal MIME type (see EmailProducer#invoke, which passes it straight through to
 * MimeBodyPart#setContent as the Content-Type) but the realistic choices for a single-part email body are just
 * text/plain and text/html - Studio now offers those as a {@link ComponentPropertyMeta#getChoices()} dropdown, but
 * with {@link ComponentPropertyMeta#isChoicesEditable()} on so a value outside that list (e.g. a charset suffix)
 * can still be typed directly - the dropdown suggests, it doesn't lock the property down.
 */
public class ComponentPropertyEditRowChoicesEditableTest {

    private static ComponentMeta emailProducerMeta;
    private static ComponentMeta moduleMeta;

    @BeforeAll
    public static void loadMeta() throws Exception {
        emailProducerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer");
        moduleMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Module");
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, new HashMap<>());
    }

    @Test
    public void emailFormatOffersTextPlainAndTextHtmlAsChoices() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        ComponentPropertyEditRow emailFormatRow = rowFor(flowElement, "emailFormat");

        assertEquals(java.util.List.of("text/plain", "text/html"), emailFormatRow.getComponentProperty().getMeta().getChoices());
    }

    @Test
    public void emailFormatsDropdownIsEditableAndAcceptsAValueOutsideTheChoices() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(emailProducerMeta)
                .componentName("My Email Producer")
                .build();
        ComponentPropertyEditRow emailFormatRow = rowFor(flowElement, "emailFormat");
        emailFormatRow.resetDataEntryComponentsWithNewValues();

        assertTrue(emailFormatRow.getInputField().getPropertyChoiceValueField().isEditable(),
                "the emailFormat dropdown must remain directly editable - a free-text escape hatch");

        // A value not in the suggested choices, e.g. a charset suffix - simulates a restored/typed custom value.
        emailFormatRow.getInputField().getPropertyChoiceValueField().setSelectedItem("text/plain; charset=utf-8");

        assertEquals("text/plain; charset=utf-8", emailFormatRow.getValue());
    }

    @Test
    public void aChoicesPropertyWithoutChoicesEditableStaysLockedToTheList() {
        // Regression guard: an existing, deliberately closed choices property (Module's flowStartupType is a real
        // enum - AUTOMATIC/MANUAL/DISABLED - not free text) must not become editable as a side effect of this
        // feature; choicesEditable defaults to false and only components that opt in get the escape hatch.
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(moduleMeta)
                .componentName("My Module")
                .build();
        ComponentPropertyEditRow flowStartupTypeRow = rowFor(flowElement, "flowStartupType");
        flowStartupTypeRow.resetDataEntryComponentsWithNewValues();

        assertFalse(flowStartupTypeRow.getInputField().getPropertyChoiceValueField().isEditable());
    }
}
