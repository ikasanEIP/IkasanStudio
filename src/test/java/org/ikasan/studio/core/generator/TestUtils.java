package org.ikasan.studio.core.generator;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public static String getFileAsString (String filenamePath) throws IOException {
        InputStream in = TestUtils.class.getResourceAsStream(filenamePath);
        if (in == null) {
            throw new IOException("Could not read file " + filenamePath);
        }
        String fileString = IOUtils.toString(in, StandardCharsets.UTF_8).replaceAll("\\r\\n", "\n");
        fileString = fileString.replaceAll("\\r", "\n");
        return fileString;
    }
}
