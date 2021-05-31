package org.ikasan.studio.generator;

import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public static String getFileAsString (String filenamePath) throws IOException {
        InputStream in = TestUtils.class.getResourceAsStream(filenamePath);
        String expectedString = StringUtil.convertLineSeparators(IOUtils.toString(in, StandardCharsets.UTF_8));
        return expectedString;
    }
}
