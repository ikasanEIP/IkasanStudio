package org.ikasan.studio.core.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Common Java 11-compatible launcher; the project supplies its own H2 runtime dependency. */
public final class IndependentH2Template {
    public static final String PACKAGE = "org.ikasan.studio.boot";
    public static final String CLASS_NAME = "IndependentH2";
    public static final String PATH = "generated/src/main/java/org/ikasan/studio/boot/IndependentH2.java";
    private IndependentH2Template() { }

    public static String create() throws IOException {
        try (var input = IndependentH2Template.class.getResourceAsStream("/studio/runtime/IndependentH2.java.txt")) {
            if (input == null) throw new IOException("Independent H2 launcher resource is missing.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
