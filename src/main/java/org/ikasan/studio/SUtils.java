package org.ikasan.studio;

/**
 * Studio Utils
 */
public class SUtils {

    /**
     * Given a string deleimited by tokens e.g. this.is.my.class.bob then get the last string, bob in thei case
     * @param delimeter to use within the string, NOTE that regex is used to split the string, so special characters like '.' will need to be escaped e.g. "\\."
     * @param input strung to analyse
     * @return The last stoken of the string or an empty sp
     */
    public static String getLastToken(String delimeter, String input) {
        String returnString = "";
        if (input != null && delimeter != null) {
            String [] tokens = input.split(delimeter, -1);
            if (tokens.length > 0) {
                returnString = tokens[tokens.length-1];
            }
        }
        return returnString;
    }

    /**
     * Pretty much what org.apache.commons.text.CaseUtils does, but we are limited by what libs Intellij
     * pull into the plugin dependencies.
     * @param input a string potentially with spaces
     * @return
     */
    public static String toJavaIdentifier(final String input) {
        if (input != null && input.length() > 0) {
            int inputStringLength = input.length();
            char inputString[] = input.toCharArray();
            int outputStringLength = 0;

            boolean toUpper = false;
            for (int inputStringIndex = 0; inputStringIndex < inputStringLength; inputStringIndex++)
            {
                if (inputString[inputStringIndex] == ' ') {
                    toUpper = true;
                    continue;
                }
                else {
                    Character current = null;
                    if (outputStringLength == 0) {
                        current = Character.toLowerCase(inputString[inputStringIndex]);
                        if (! (Character.isJavaIdentifierStart(current))) {
                            System.out.println("Noo");
                            continue;
                        }
                    } else {
                        if (toUpper) {
                            current = Character.toUpperCase(inputString[inputStringIndex]);
                            if (!Character.isJavaIdentifierPart(current)) {
                                continue;
                            }
                            toUpper = false;
                        } else {
                            current = Character.toLowerCase(inputString[inputStringIndex]);
                            if (!Character.isJavaIdentifierPart(current)) {
                                continue;
                            }
                        }
                    }
                    inputString[outputStringLength++] = current;
                }
            }
            return String.valueOf(inputString, 0, outputStringLength);
        } else {
            return "";
        }

    }
}
