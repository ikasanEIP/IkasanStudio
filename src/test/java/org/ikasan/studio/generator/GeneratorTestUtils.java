package org.ikasan.studio.generator;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.meta.ComponentType;

import java.io.File;
import java.io.IOException;

public class GeneratorTestUtils {

    public static String getExptectedFreemarkerOutputFromTestFile (String expectedTestResultFileName) throws IOException {
        String testFile = Context.FREEMARKER_TEMPLATE_PATH + getComponentTypeFromPath(expectedTestResultFileName) + expectedTestResultFileName ;
        return TestUtils.getFileAsString(testFile);
    }

    public static String getComponentTypeFromPath(String fileName) {
        String searchString = fileName.replace("Flow","").replace("Module", "");
        return ComponentType.parseComponentTypeContains(searchString).toString() + File.separator;
    }
}
