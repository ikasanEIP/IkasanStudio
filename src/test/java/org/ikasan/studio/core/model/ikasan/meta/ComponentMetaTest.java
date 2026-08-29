package org.ikasan.studio.core.model.ikasan.meta;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers ComponentMeta#getEffectiveInputTypeDescriptionPreview / #getEffectiveOutputTypeDescriptionPreview -
 * the metadata-only variant of FlowElement#getEffectiveInputTypeDescription / #getEffectiveOutputTypeDescription
 * used where there's no FlowElement instance yet to read live property values from (a palette item, see
 * IkasanPaletteElementViewHandler#getHelpText) - falls back to each property's own declared default value
 * instead, i.e. what a freshly-dropped instance would start with before the user changes anything.
 */
@ExtendWith(SharedResourceExtension.class)
class ComponentMetaTest {

    @Test
    public void preview_uses_the_fromType_and_toType_property_defaults_for_a_converter() throws StudioBuildException {
        ComponentMeta converter = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Converter");

        assertEquals("java.lang.String", converter.getEffectiveInputTypeDescriptionPreview());
        assertEquals("java.lang.String", converter.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void preview_input_is_always_null_for_a_consumer() throws StudioBuildException {
        ComponentMeta localFileConsumer = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Local File Consumer");

        assertNull(localFileConsumer.getEffectiveInputTypeDescriptionPreview());
    }

    @Test
    public void preview_output_uses_the_fixed_producedOutputType_for_a_consumer_with_no_toType_property() throws StudioBuildException {
        ComponentMeta localFileConsumer = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Local File Consumer");

        assertEquals("java.util.List<java.io.File>", localFileConsumer.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void preview_output_is_null_for_a_producer_since_it_is_terminal() throws StudioBuildException {
        ComponentMeta devNullProducer = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Dev Null Producer");

        assertNull(devNullProducer.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void preview_output_for_a_router_mirrors_its_own_input_default_not_its_toType_default() throws StudioBuildException {
        // MultiRecipientRouter's own toType default ("java.util.List<java.lang.String>") is the routing
        // decision, not the payload - the payload passes through unchanged, so output must mirror fromType.
        ComponentMeta router = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Multi Recipient Router");

        assertEquals(router.getEffectiveInputTypeDescriptionPreview(), router.getEffectiveOutputTypeDescriptionPreview());
    }
}
