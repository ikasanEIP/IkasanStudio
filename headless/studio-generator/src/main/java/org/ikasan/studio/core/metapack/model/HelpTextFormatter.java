package org.ikasan.studio.core.metapack.model;

/** Composes metadata help fragments into consistent Swing-compatible HTML paragraphs. */
public final class HelpTextFormatter {
    private HelpTextFormatter() {
    }

    public static String categoryThenComponent(String categoryHelp, String componentHelp) {
        String category = asParagraph(categoryHelp);
        String component = asParagraph(componentHelp);
        if (category.equals(component)) {
            return category;
        }
        return category + component;
    }

    private static String asParagraph(String helpText) {
        if (helpText == null || helpText.isBlank()) {
            return "";
        }
        String trimmed = helpText.strip();
        if (startsWithHtmlBlock(trimmed)) {
            return trimmed;
        }
        return "<p>" + trimmed + "</p>";
    }

    private static boolean startsWithHtmlBlock(String text) {
        String lowerCase = text.toLowerCase();
        return lowerCase.startsWith("<p")
                || lowerCase.startsWith("<div")
                || lowerCase.startsWith("<ul")
                || lowerCase.startsWith("<ol")
                || lowerCase.startsWith("<table")
                || lowerCase.startsWith("<h1")
                || lowerCase.startsWith("<h2")
                || lowerCase.startsWith("<h3")
                || lowerCase.startsWith("<html");
    }
}
