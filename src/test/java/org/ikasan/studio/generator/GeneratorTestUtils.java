package org.ikasan.studio.generator;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.BasicElement;

import java.io.IOException;
import java.nio.file.FileSystems;

public class GeneratorTestUtils {

    public static String getExptectedFreemarkerOutputFromTestFile(BasicElement basicElement, String expectedTestResultFileName) throws IOException {
        String testFile = Context.FREEMARKER_TEMPLATE_PATH + getSavedPathFromFlowElement(basicElement) + FileSystems.getDefault().getSeparator() + expectedTestResultFileName;
        return TestUtils.getFileAsString(testFile);
    }


    //    public static String getComponentTypeFromPath(String fileName) {
//        String searchString = fileName.replace("Flow","").replace("Module", "");
//        return ComponentType.parseComponentTypeContains(searchString).toString() + File.separator;
//    }
//
    public static String getSavedPathFromFlowElement(BasicElement basicElement) {
        if (basicElement == null || basicElement.getComponentMeta() == null || basicElement.getComponentMeta().getComponentShortType() == null) {
            return "";
        } else {
            return basicElement.getComponentMeta().getComponentShortType().replace(" ", "");
        }
    }
}

