package org.ikasan.studio.core.generation;

/** Pure naming rules shared by generators and templates. */
public final class JavaSourceNames {
    private JavaSourceNames() {
    }

    public static String toClassName(String input) {
        String identifier = toIdentifier(input);
        if (identifier.isEmpty()) {
            return identifier;
        }
        return Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);
    }

    public static String toPackageName(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String prefixed = Character.isDigit(input.charAt(0)) ? "_" + input : input;
        return prefixed.replaceAll("[^a-zA-Z0-9_]+", "").toLowerCase();
    }

    public static String toIdentifier(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        char[] characters = input.toCharArray();
        int outputLength = 0;
        boolean uppercaseNext = false;
        for (char character : input.toCharArray()) {
            if (character == ' ' || character == '.') {
                uppercaseNext = true;
                continue;
            }
            char candidate = outputLength == 0 ? Character.toLowerCase(character)
                    : uppercaseNext ? Character.toUpperCase(character) : character;
            if (outputLength == 0 ? !Character.isJavaIdentifierStart(candidate)
                    : !Character.isJavaIdentifierPart(candidate)) {
                continue;
            }
            characters[outputLength++] = candidate;
            uppercaseNext = false;
        }
        return String.valueOf(characters, 0, outputLength);
    }
}
