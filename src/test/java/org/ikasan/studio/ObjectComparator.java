package org.ikasan.studio;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ObjectComparator {

    public static List<String> compareFieldsExcept(Object a, Object b, List<String> excludedFields) {
        if (a == null || b == null) throw new IllegalArgumentException("Objects must not be null");
        if (!a.getClass().equals(b.getClass())) throw new IllegalArgumentException("Objects must be of same class");

        List<String> differences = new ArrayList<>();
        Field[] fields = a.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (excludedFields.contains(field.getName())) continue;

            field.setAccessible(true);
            try {
                Object valA = field.get(a);
                Object valB = field.get(b);

                // Special handling for Strings
                if (valA instanceof String && valB instanceof String) {
                    String strA = ((String) valA).trim();
                    String strB = ((String) valB).trim();
                    if (!strA.equalsIgnoreCase(strB)) {
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
