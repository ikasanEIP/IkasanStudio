package org.ikasan.studio.generator;

import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.IOUtils;
import org.ikasan.studio.model.ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.IkasanComponentPropertyMetaKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeSet;

public class TestUtils {

    public static String getFileAsString (String filenamePath) throws IOException {
        InputStream in = TestUtils.class.getResourceAsStream(filenamePath);
        String expectedString = StringUtil.convertLineSeparators(IOUtils.toString(in, StandardCharsets.UTF_8));
        return expectedString;
    }

    public static String getConfiguredPropertyValues(Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> configuredProperties) {
        StringBuilder returnString = new StringBuilder();
        if (configuredProperties != null && !configuredProperties.isEmpty()) {
            TreeSet<IkasanComponentPropertyMetaKey> keys = new TreeSet<>(configuredProperties.keySet());
            for (IkasanComponentPropertyMetaKey key : keys) {
                Object value = configuredProperties.get(key);
                String displayValue = "null";
                if (value != null) {
                    displayValue = ((IkasanComponentProperty) value).getValueString();
                }
                returnString.append(key.getPropertyName()).append("->").append(displayValue).append(",");
            }
        }
        return returnString.toString();
    }
}
