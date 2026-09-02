package org.ikasan.studio.core.metapack.model;

import org.ikasan.studio.core.metapack.ComponentLibrary;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        ComponentMeta converter = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Converter");

        assertEquals("java.lang.String", converter.getEffectiveInputTypeDescriptionPreview());
        assertEquals("java.lang.String", converter.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void preview_input_is_always_null_for_a_consumer() throws StudioBuildException {
        ComponentMeta localFileConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Local File Consumer");

        assertNull(localFileConsumer.getEffectiveInputTypeDescriptionPreview());
    }

    @Test
    public void preview_output_uses_the_fixed_producedOutputType_for_a_consumer_with_no_toType_property() throws StudioBuildException {
        ComponentMeta localFileConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Local File Consumer");

        assertEquals("java.util.List<java.io.File>", localFileConsumer.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void output_is_the_raw_jms_message_type_when_auto_content_conversion_is_off() throws StudioBuildException {
        ComponentMeta basicAmqSpringJmsConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Basic AMQ Spring JMS Consumer");

        String output = basicAmqSpringJmsConsumer.getEffectiveOutputTypeDescription(
                propertyName -> "autoContentConversion".equals(propertyName) ? "false" : "");

        assertEquals("javax.jms.Message", output);
    }

    @Test
    public void output_reflects_the_unwrapped_payload_type_when_auto_content_conversion_is_on() throws StudioBuildException {
        // With Auto Content Conversion on, JmsMessageConverter unwraps the raw message before the flow ever
        // sees it - the declared producedOutputType (javax.jms.Message) is never what's actually delivered
        // once this is true, so the description must change to reflect that, not keep claiming the raw type.
        ComponentMeta basicAmqSpringJmsConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Basic AMQ Spring JMS Consumer");

        String output = basicAmqSpringJmsConsumer.getEffectiveOutputTypeDescription(
                propertyName -> "autoContentConversion".equals(propertyName) ? "true" : "");

        assertTrue(output.contains("java.lang.Object"));
        assertFalse(output.contains("jms.Message"));
    }

    @Test
    public void preview_output_is_null_for_a_producer_since_it_is_terminal() throws StudioBuildException {
        ComponentMeta devNullProducer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Dev Null Producer");

        assertNull(devNullProducer.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void preview_output_for_a_router_mirrors_its_own_input_default_not_its_toType_default() throws StudioBuildException {
        // MultiRecipientRouter's own toType default ("java.util.List<java.lang.String>") is the routing
        // decision, not the payload - the payload passes through unchanged, so output must mirror fromType.
        ComponentMeta router = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Multi Recipient Router");

        assertEquals(router.getEffectiveInputTypeDescriptionPreview(), router.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void input_description_is_null_not_blank_for_a_producer_with_no_fromType_property_at_all() throws StudioBuildException {
        // Basic AMQ JMS Producer wraps its implementingClass directly - it declares neither fromType nor a
        // custom expectedInputTypeProperty. The palette preview path (getDefaultValueAsString) already
        // correctly returns null for a property that doesn't exist. A live FlowElement's own resolver
        // (BasicElement#getPropertyValueAsString) instead returns "" for a property that doesn't exist at all
        // (as opposed to one that exists but is unset) - simulated here directly against the shared engine
        // method, since that's the exact distinction the fix depends on. Regression test for a stray "Input:"
        // with nothing after it showing up in the properties panel/canvas summary for any such component.
        ComponentMeta basicAmqJmsProducer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Basic AMQ JMS Producer");

        assertNull(basicAmqJmsProducer.getEffectiveInputTypeDescription(propertyName -> ""));
    }

    @Test
    public void preview_output_uses_the_fixed_producedOutputType_for_the_email_converter_since_it_has_no_toType_property() throws StudioBuildException {
        ComponentMeta emailConverter = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Converter");

        assertEquals("org.ikasan.component.endpoint.email.producer.EmailPayload", emailConverter.getEffectiveOutputTypeDescriptionPreview());
    }

    @Test
    public void producesFileListPayload_is_true_for_local_file_consumer() throws StudioBuildException {
        ComponentMeta localFileConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Local File Consumer");

        assertTrue(localFileConsumer.producesFileListPayload());
    }

    @Test
    public void producesFileListPayload_is_false_for_ftp_consumer_since_its_real_payload_is_a_richer_transfer_object() throws StudioBuildException {
        // FTP/SFTP Consumer's producedOutputType is org.ikasan.filetransfer.Payload, not java.util.List<java.io.File> -
        // isFileBasedConsumer() is still true for it (it drives the badge icon only), but a local file picker
        // can't honestly stand in for that richer transfer-metadata object.
        ComponentMeta ftpConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "FTP Consumer");

        assertTrue(ftpConsumer.isFileBasedConsumer());
        assertFalse(ftpConsumer.producesFileListPayload());
    }

    @Test
    public void producesFileListPayload_is_false_for_generic_consumer_since_it_has_no_fixed_output_type() throws StudioBuildException {
        ComponentMeta genericConsumer = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Generic Consumer");

        assertTrue(genericConsumer.isFileBasedConsumer());
        assertFalse(genericConsumer.producesFileListPayload());
    }

    @Test
    public void producesFileListPayload_is_false_for_a_non_file_component() throws StudioBuildException {
        ComponentMeta converter = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Converter");

        assertFalse(converter.producesFileListPayload());
    }
}
