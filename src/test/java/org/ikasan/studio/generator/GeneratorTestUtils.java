package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.Context;

import java.io.IOException;

public class GeneratorTestUtils  extends TestCase {

    public static String getExptectedFreemarkerOutputFromTestFile (String expectedTestResultFileName) throws IOException {
        String testFile = Context.FREEMARKER_TEMPLATE_PATH + expectedTestResultFileName;
        return TestUtils.getFileAsString(testFile);
    }
}
