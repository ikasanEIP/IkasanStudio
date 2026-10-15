package org.example.acap.generictoftpflow;

import org.ikasan.filetransfer.FilePayloadAttributeNames;
import org.ikasan.filetransfer.Payload;
import org.ikasan.filetransfer.component.DefaultPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * String to FTP/SFTP Payload. Unsupported content is rejected; provide a custom mapping when needed.
 */
@Component("org.example.acap.generictoftpflow.MyGenToFtpConverter")
public class MyGenToFtpConverter implements Converter<String, Payload> {
    public static final String FILENAME_PREFIX = "flow_3-my_gen_to_ftp_converter-";

    @Override
    public Payload convert(String source) throws TransformationException {
        if (source == null) throw new TransformationException("Cannot convert null content");
        try {
            // Blank unless the developer has explicitly configured a fixed name - each construction template
            // (see construct-file.ftl/construct-email-attachment.ftl) applies its own independent fallback when
            // this is still blank at the point it's used, since FTP/SFTP and email attachments need different
            // fallback behaviour. Extraction templates (e.g. extract-file.ftl) may also overwrite this with a
            // filename preserved from the incoming payload, which always takes priority over both.
            String filename = "";
            Charset charset = Charset.forName("UTF-8");
            Object body = source;
            // No filename preserved from upstream and none explicitly configured (see recipeFilename) - many
            // FTP/SFTP servers, including strict test emulators, refuse to overwrite an existing file and would
            // stop the flow the moment a fixed/reused name collided with one already delivered. Generate one
            // that's unique on every call instead: it also carries the flow and converter name, so with several
            // converters delivering into the same directory you can tell which one produced which file, and the
            // timestamp doubles as a delivery history.
            if (filename.isBlank()) {
                filename = FILENAME_PREFIX
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
