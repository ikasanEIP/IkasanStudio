package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.BuildContext;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;

import java.io.IOException;

public class GeneratorTestUtils {

    public static String getExptectedFreemarkerOutputFromTestFile(BasicElement basicElement, String expectedTestResultFileName) throws IOException {
        String testFile = BuildContext.FREEMARKER_OUTPUT_PATH + getSavedPathFromFlowElement(basicElement) + "/" + expectedTestResultFileName;
        return TestUtils.getFileAsString(testFile);
    }

    public static String getSavedPathFromFlowElement(BasicElement basicElement) {
        if (basicElement == null || basicElement.getComponentMeta() == null || basicElement.getComponentMeta().getComponentTypeMeta().getComponentShortType() == null) {
            return "";
        } else {
            return basicElement.getComponentMeta().getComponentTypeMeta().getComponentShortType().replace(" ", "");
        }
    }
}

