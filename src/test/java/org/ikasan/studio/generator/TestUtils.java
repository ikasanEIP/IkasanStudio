package org.ikasan.studio.generator;

import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.IOUtils;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TestUtils {

    public static String getFileAsString (String filenamePath) throws IOException {
        InputStream in = TestUtils.class.getResourceAsStream(filenamePath);
        String expectedString = StringUtil.convertLineSeparators(IOUtils.toString(in, StandardCharsets.UTF_8));
        return expectedString;
    }

    public static String getConfiguredPropertyValues(Map<String, IkasanComponentProperty> configuredProperties) {
        if (configuredProperties != null && !configuredProperties.isEmpty()) {
            TreeSet<String> keys = new TreeSet<>(configuredProperties.keySet());

            return keys.stream()
                .filter(x -> configuredProperties.get(x)!=null)
                .map(x -> x + "->" + configuredProperties.get(x).getValueString())
                .collect(Collectors.joining(","));
        }
        return "";
    }
}
