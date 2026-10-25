package org.ikasan.studio.demo.cat.transformtheorder;

/**
 * Builds the EmailPayload the Email Producer needs to send a message. EmailPayload.newInstance() always
 * returns a DefaultEmailPayload - cast to it to reach setEmailBody(...) and addAttachment(name, type, bytes),
 * neither of which are on the EmailPayload interface itself.
 * <p>
 * Unlike Broker, this component detects "full event" mode safely: set the input type below to the real payload
 * type (e.g. String) to receive just the payload, or to org.ikasan.spec.flow.FlowEvent to receive the full event
 * (call event.getPayload() inside convert() to get the payload out) - Ikasan checks your class for a literal
 * convert(FlowEvent) method rather than catching a runtime cast failure, so java.lang.Object is not a trap here.
 * <p>
 * This is an auto generated stub. The user is expected to fill in the details of the conversion below.
 * This stub will not be overwritten unless the overwrite checkbox is explicitly selected.
 */

import org.ikasan.component.endpoint.email.producer.DefaultEmailPayload;
import org.ikasan.component.endpoint.email.producer.EmailPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("org.ikasan.studio.demo.cat.transformtheorder.AddToEmail")
public class AddToEmail implements Converter<String, EmailPayload> {
    private static final Logger LOG = LoggerFactory.getLogger(AddToEmail.class);

    public EmailPayload convert(String payload) throws TransformationException {
// Uncomment for diagnostics without logging message contents.
// LOG.debug("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
        DefaultEmailPayload emailPayload = (DefaultEmailPayload) EmailPayload.newInstance();
        emailPayload.setEmailBody(payload.toString());
// TODO review the default body above, and populate any attachments, from the incoming payload, e.g.
// emailPayload.addAttachment("report.pdf", "application/pdf", attachmentBytes);
        return emailPayload;
    }
}