package org.ikasan.studio.core.generation;

import javax.lang.model.SourceVersion;
import java.util.Locale;

/** Pure naming rules shared by generators and templates. */
public final class JavaSourceNames {
    private static final String FALLBACK = "unnamed";

    private JavaSourceNames() {
    }

    public static String toClassName(String input) {
        String identifier = toIdentifier(input);
        if (identifier.isEmpty()) {
            return identifier;
        }
        return Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);
    }

    /**
     * A package name segment. A Java keyword such as "import" or "default" is not a legal segment, so it is
     * given a trailing underscore. Every other result is unchanged.
     */
    public static String toPackageName(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String prefixed = Character.isDigit(input.charAt(0)) ? "_" + input : input;
        String name = prefixed.replaceAll("[^a-zA-Z0-9_]+", "").toLowerCase(Locale.ROOT);
        return SourceVersion.isKeyword(name) ? name + "_" : name;
    }

    /**
     * A name that is legal wherever an identifier is: a name that yields nothing usable (e.g. "123") is given a
     * leading underscore, or a fixed fallback if there is still nothing. A name that already yields something is
     * unchanged, so existing generated code is not renamed.
     */
    public static String toIdentifier(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String identifier = scan(input);
        if (identifier.isEmpty()) {
            identifier = scan("_" + input);
        }
        return identifier.isEmpty() || identifier.equals("_") ? FALLBACK : identifier;
    }

    /**
     * A name for a variable or field. It is the identifier, except that a Java keyword (a flow or component called
     * "Default" or "New" gives "default"/"new") cannot be a variable name so is given a trailing underscore.
     * Class names are capitalised and so are never keywords: use {@link #toClassName} for those.
     */
    public static String toVariableName(String input) {
        String identifier = toIdentifier(input);
        return SourceVersion.isKeyword(identifier) ? identifier + "_" : identifier;
    }

    private static String scan(String input) {
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
