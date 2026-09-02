package org.ikasan.studio.core.metapack.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelpTextFormatterTest {
    @Test
    void putsCategoryBeforeComponentInSeparateParagraphs() {
        assertThat(HelpTextFormatter.categoryThenComponent("Category help", "Component help"))
                .isEqualTo("<p>Category help</p><p>Component help</p>");
    }

    @Test
    void preservesExistingHtmlParagraphs() {
        assertThat(HelpTextFormatter.categoryThenComponent(
                "<p>Category one.</p><p>Category two.</p>", "<p>Component.</p>"))
                .isEqualTo("<p>Category one.</p><p>Category two.</p><p>Component.</p>");
    }

    @Test
    void handlesEitherMissingFragment() {
        assertThat(HelpTextFormatter.categoryThenComponent("Category", null))
                .isEqualTo("<p>Category</p>");
        assertThat(HelpTextFormatter.categoryThenComponent("", "Component"))
                .isEqualTo("<p>Component</p>");
    }

    @Test
    void doesNotRepeatIdenticalCategoryAndComponentHelp() {
        assertThat(HelpTextFormatter.categoryThenComponent("<p>Shared help.</p>", "<p>Shared help.</p>"))
                .isEqualTo("<p>Shared help.</p>");
    }
}
