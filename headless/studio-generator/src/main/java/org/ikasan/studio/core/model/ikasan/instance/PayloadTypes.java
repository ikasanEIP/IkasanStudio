package org.ikasan.studio.core.model.ikasan.instance;

import java.util.ArrayList;
import java.util.List;

/** Checks declared payload types without loading project classes or accessing IDE indexes. */
final class PayloadTypes {
    private PayloadTypes() { }

    static List<String> alternatives(String description) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < description.length(); i++) {
            char c = description.charAt(i);
            if (c == '<') depth++;
            if (c == '>') depth--;
            if (c == ',' && depth == 0) {
                result.add(description.substring(start, i).trim());
                start = i + 1;
            }
        }
        String last = description.substring(start).trim();
        if (!last.isEmpty()) result.add(last);
        return result;
    }

    static String raw(String type) {
        return type.split("[<(]", 2)[0].trim();
    }

    static boolean unknown(String type) {
        if (type == null || type.isBlank()) return true;
        String raw = raw(type);
        String simple = raw.substring(raw.lastIndexOf('.') + 1);
        return simple.equals("Object") || simple.equals("FlowEvent");
    }

    static boolean accepts(String expected, String actual) {
        if (unknown(actual) || unknown(expected)) return true;
        String target = raw(expected);
        String source = raw(actual);
        if (differentNames(target, source)) {
            Class<?> targetClass = jdkClass(target);
            Class<?> sourceClass = jdkClass(source);
            if (targetClass == null || sourceClass == null || !targetClass.isAssignableFrom(sourceClass)) return false;
        }
        List<String> expectedArguments = arguments(expected);
        List<String> actualArguments = arguments(actual);
        // Raw declarations do not establish their element types.
        if (expectedArguments.isEmpty() || actualArguments.isEmpty()) return true;
        if (expectedArguments.size() != actualArguments.size()) return false;
        for (int i = 0; i < expectedArguments.size(); i++) {
            String e = expectedArguments.get(i);
            String a = actualArguments.get(i);
            // Bounds and type variables require project resolution; avoid speculative warnings.
            if (e.contains("?") || a.contains("?") || e.length() == 1 || a.length() == 1) continue;
            if (differentNames(raw(e), raw(a)) || !accepts(e, a)) return false;
        }
        return true;
    }

    private static boolean differentNames(String a, String b) {
        if (a.equals(b)) return false;
        if (!a.contains(".")) return !a.equals(b.substring(b.lastIndexOf('.') + 1));
        if (!b.contains(".")) return !b.equals(a.substring(a.lastIndexOf('.') + 1));
        return true;
    }

    static List<String> arguments(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        return start < 0 || end <= start ? List.of() : alternatives(type.substring(start + 1, end));
    }

    private static Class<?> jdkClass(String name) {
        if (!name.contains(".")) name = "java.lang." + name;
        if (!name.startsWith("java.")) return null;
        try {
            return Class.forName(name, false, null);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}
