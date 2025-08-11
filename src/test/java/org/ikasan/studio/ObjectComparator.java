package org.ikasan.studio;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ObjectComparator {

    /**
     * Attempts to compare the attributes of two objects of the same class, excluding specified attributes.
     * This method uses reflection to access the fields of the objects and compares their string representation, this is limited.
     * It returns a list of differences found in the attributes, formatted as strings.
     * @param first object to compare
     * @param second object to compare
     * @param excludedAttributes to ignore during comparison
     * @return list of differences found in the attributes, formatted as strings.
     */
    public static List<String> compareAttributesExcept(Object first, Object second, List<String> excludedAttributes) {
        if (first == null || second == null) throw new IllegalArgumentException("Objects must not be null");
        if (!first.getClass().equals(second.getClass())) throw new IllegalArgumentException("Objects must be of same class");

        List<String> differences = new ArrayList<>();
        Field[] fields = first.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (excludedAttributes.contains(field.getName())) continue;

            field.setAccessible(true);
            try {
                Object valA = field.get(first);
                Object valB = field.get(second);

                // Special handling for Strings
                if (valA instanceof String && valB instanceof String) {
                    String strA = ((String) valA).trim();
                    String strB = ((String) valB).trim();
                    if (!strA.equals(strB)) {
                        differences.add(String.format("Field '%s': \"%s\" != \"%s\"", field.getName(), strA, strB));
                    }

                    // Fallback: compare using toString (null-safe)
                } else {
                    String strA = (valA == null) ? "null" : valA.toString();
                    String strB = (valB == null) ? "null" : valB.toString();

                    if (!strA.equals(strB)) {
                        differences.add(String.format("Field '%s': %s != %s", field.getName(), strA, strB));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field " + field.getName(), e);
            }
        }

        return differences;
    }
}
