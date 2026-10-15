package org.example.acap.sftptojmsflow;

import org.ikasan.filetransfer.Payload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * FTP/SFTP Payload to String. Unsupported content is rejected; provide a custom mapping when needed.
 */
@Component("org.example.acap.sftptojmsflow.MyConverter")
public class MyConverter implements Converter<Payload, String> {
    @Override
    public String convert(Payload source) throws TransformationException {
        if (source == null) throw new TransformationException("Cannot convert null content");
        try {
            String filename = "message.dat";
            Charset charset = Charset.forName("UTF-8");
            Object body = source.getContent();
            String originalFilename = source.getAttribute("fileName");
            if (originalFilename != null && !originalFilename.isBlank()) filename = originalFilename;
            return text(body, charset);
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
