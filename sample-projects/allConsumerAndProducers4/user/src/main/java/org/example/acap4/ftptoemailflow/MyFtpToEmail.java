package org.example.acap4.ftptoemailflow;

import org.ikasan.component.endpoint.email.producer.EmailPayload;
import org.ikasan.filetransfer.Payload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * FTP/SFTP Payload to Email body. Unsupported content is rejected; provide a custom mapping when needed.
 */
@Component("org.example.acap4.ftptoemailflow.MyFtpToEmail")
public class MyFtpToEmail implements Converter<Payload, EmailPayload> {
    private static final Logger LOG = LoggerFactory.getLogger(MyFtpToEmail.class);

    @Override
    public EmailPayload convert(Payload source) throws TransformationException {
// Uncomment for diagnostics without logging message contents.
// LOG.debug("Processing payload type {}", source == null ? "null" : source.getClass().getName());
        if (source == null) throw new TransformationException("Cannot convert null content");
        try {
            // Blank unless the developer has explicitly configured a fixed name - each construction template
            // (see construct-file.ftl/construct-email-attachment.ftl) applies its own independent fallback when
            // this is still blank at the point it's used, since FTP/SFTP and email attachments need different
            // fallback behaviour. Extraction templates (e.g. extract-file.ftl) may also overwrite this with a
            // filename preserved from the incoming payload, which always takes priority over both.
            String filename = "";
            Charset charset = Charset.forName("UTF-8");
            Object body = source.getContent();
            String originalFilename = source.getAttribute("fileName");
            if (originalFilename != null && !originalFilename.isBlank()) filename = originalFilename;
            EmailPayload result = EmailPayload.newInstance();
            result.setEmailBody(text(body, charset));
            return result;
        } catch (TransformationException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformationException("Conversion failed; check the recipe and incoming content type", e);
        }
    }

    private byte[] bytes(Object body, Charset charset) throws Exception {
        if (body instanceof byte[]) return (byte[]) body;
        if (body instanceof String) {
            ByteBuffer encoded = charset.newEncoder().encode(CharBuffer.wrap((String) body));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        }
        throw new TransformationException("Expected String or byte[] content; select a custom converter for structured objects");
    }

    private String text(Object body, Charset charset) throws Exception {
        if (body instanceof String) return (String) body;
        return charset.newDecoder().decode(ByteBuffer.wrap(bytes(body, charset))).toString();
    }
}
