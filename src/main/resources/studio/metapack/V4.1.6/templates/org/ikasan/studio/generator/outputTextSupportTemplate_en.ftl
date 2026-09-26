package org.ikasan.studio.flowtests;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Content conversion for assertions; reads values without acknowledging messages or consuming streams. */
public final class OutputTextSupport {
    private OutputTextSupport() { }

    /**
     * Decodes UTF-8 bytes, Ikasan Payload content, local files/paths, file lists and JMS TextMessage bodies.
     * File lists retain their existing order and concatenate contents without adding separators.
     * Unsupported/binary objects require an explicit test override, never an identity-string comparison.
     */
    public static String stringify(Object value) {
        try {
            if (value == null) return "null";
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Character)
                return value.toString();
            if (value instanceof byte[]) return decode((byte[]) value);
            if (value instanceof File) return Files.readString(((File) value).toPath(), StandardCharsets.UTF_8);
            if (value instanceof Path) return Files.readString((Path) value, StandardCharsets.UTF_8);
            if (value instanceof List<?> && ((List<?>) value).stream().allMatch(item -> item instanceof File)) {
                StringBuilder text = new StringBuilder();
                for (Object file : (List<?>) value) text.append(stringify(file));
                return text.toString();
            }
            // Optional endpoint APIs need not be dependencies of modules which do not use them.
            // Match the interface contract, including proxies/subclasses, rather than an implementation name.
            Class<?> payload = findInterface(value.getClass(), "org.ikasan.filetransfer.Payload");
            if (payload != null) return decode((byte[]) payload.getMethod("getContent").invoke(value));
            Class<?> message = findInterface(value.getClass(), "jakarta.jms.TextMessage");
            if (message != null) return String.valueOf(message.getMethod("getText").invoke(value));
            throw new IllegalArgumentException("No text-content adapter for " + value.getClass().getName()
                    + ". Override outputText(Object) for this payload. JMS supports TextMessage only; binary/object messages need an explicit mapping.");
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new IllegalArgumentException("Cannot read output content from " + value.getClass().getName(), failure.getCause());
        } catch (java.io.IOException | ReflectiveOperationException failure) {
            throw new IllegalArgumentException("Cannot decode output content; use UTF-8 text or override outputText(Object)", failure);
        }
    }

    /** Reject malformed UTF-8 instead of silently replacing bytes and hiding a conversion defect. */
    private static String decode(byte[] bytes) throws java.nio.charset.CharacterCodingException {
        if (bytes == null) throw new IllegalArgumentException("Payload content is null; supply an explicit null-content assertion");
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static Class<?> findInterface(Class<?> type, String name) {
        if (type == null) return null;
        if (type.isInterface() && type.getName().equals(name)) return type;
        for (Class<?> candidate : type.getInterfaces()) {
            Class<?> match = findInterface(candidate, name);
            if (match != null) return match;
        }
        return findInterface(type.getSuperclass(), name);
    }
}
