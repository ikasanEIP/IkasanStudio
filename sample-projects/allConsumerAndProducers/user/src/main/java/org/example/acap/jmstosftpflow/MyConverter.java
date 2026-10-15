package org.example.acap.jmstosftpflow;

import org.ikasan.filetransfer.FilePayloadAttributeNames;
import org.ikasan.filetransfer.Payload;
import org.ikasan.filetransfer.component.DefaultPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JMS Message to FTP/SFTP Payload. Unsupported content is rejected; provide a custom mapping when needed.
 */
@Component("org.example.acap.jmstosftpflow.MyConverter")
public class MyConverter implements Converter<Message, Payload> {
    private static final Logger LOG = LoggerFactory.getLogger(MyConverter.class);

    @Override
    public Payload convert(Message payload) throws TransformationException {
// Uncomment for diagnostics without logging message contents.
// LOG.debug("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
        if (payload == null) throw new TransformationException("Cannot convert null content");
        try {
            // Blank unless the developer has explicitly configured a fixed name - each construction template
            // (see construct-file.ftl/construct-email-attachment.ftl) applies its own independent fallback when
            // this is still blank at the point it's used, since FTP/SFTP and email attachments need different
            // fallback behaviour. Extraction templates (e.g. extract-file.ftl) may also overwrite this with a
            // filename preserved from the incoming payload, which always takes priority over both.
            String filename = "";
            Charset charset = Charset.forName("UTF-8");
            Object body;
            if (payload instanceof TextMessage) {
                body = ((TextMessage) payload).getText();
            } else if (payload instanceof BytesMessage) {
                BytesMessage jmsMessage = (BytesMessage) payload;
                jmsMessage.reset();
                try {
                    long length = jmsMessage.getBodyLength();
                    if (length < 0 || length > 16 * 1024 * 1024)
                        throw new TransformationException("JMS body exceeds the recipe's 16 MiB limit; use a streaming custom converter");
                    ByteArrayOutputStream output = new ByteArrayOutputStream((int) length);
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = jmsMessage.readBytes(buffer)) != -1) {
                        if (count == 0 || output.size() + count > 16 * 1024 * 1024)
                            throw new TransformationException("Invalid or oversized JMS byte stream");
                        output.write(buffer, 0, count);
                    }
                    body = output.toByteArray();
                } finally {
                    jmsMessage.reset();
                }
            } else if (payload instanceof MapMessage) {
                MapMessage jmsMessage = (MapMessage) payload;
                Map<String, Object> values = new LinkedHashMap<>();
                Enumeration<?> names = jmsMessage.getMapNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    values.put(name, jmsMessage.getObject(name));
                }
                body = values;
            } else {
                throw new TransformationException("Unsupported JMS message; use a custom converter for ObjectMessage or StreamMessage");
            }
            // No filename preserved from upstream and none explicitly configured (see recipeFilename) - many
            // FTP/SFTP servers, including strict test emulators, refuse to overwrite an existing file and would
            // stop the flow the moment a fixed/reused name collided with one already delivered. Generate one
            // that's unique on every call instead: it also carries the flow and converter name, so with several
            // converters delivering into the same directory you can tell which one produced which file, and the
            // timestamp doubles as a delivery history.
            if (filename.isBlank()) {
                filename = "flow7-my_converter-"
                        + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now())
                        + "-" + UUID.randomUUID() + ".dat";
            }
            DefaultPayload result = new DefaultPayload(UUID.randomUUID().toString(), bytes(body, charset));
            result.setAttribute(FilePayloadAttributeNames.FILE_NAME, filename);
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
