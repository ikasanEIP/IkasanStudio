package org.ikasan.studio.generator;

import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GeneratorTestUtils {

    public static String getExptectedVelocityOutputFromTestFile (String expectedTestResultFileName) throws IOException {
        String testFile = "/" + VelocityUtils.VELOCITY_TEMPLATE_PATH + expectedTestResultFileName;
        InputStream in = GeneratorTestUtils.class.getResourceAsStream(testFile);
        String expectedString = StringUtil.convertLineSeparators(IOUtils.toString(in, StandardCharsets.UTF_8));
        return expectedString;
    }
}
