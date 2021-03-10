package org.ikasan.studio.generator;

import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.IOUtils;
import org.ikasan.studio.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GeneratorTestUtils {

    public static String getExptectedFreemarkerOutputFromTestFile (String expectedTestResultFileName) throws IOException {
        String testFile = Context.FREEMARKER_TEMPLATE_PATH + expectedTestResultFileName;
        InputStream in = GeneratorTestUtils.class.getResourceAsStream(testFile);
        String expectedString = StringUtil.convertLineSeparators(IOUtils.toString(in, StandardCharsets.UTF_8));
        return expectedString;
    }
}
