package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SUISUtilsTest {
    @Test
    public void test_splitStringIntoMultipleRows_simple_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("the fat cat", 3);
        assertThat(actual.size(), is(3));
        assertThat(actual.get(0), is("the"));
        assertThat(actual.get(1), is("fat"));
        assertThat(actual.get(2), is("cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_unequal_word_length_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("thhe faaaat cat", 3);
        assertThat(actual.size(), is(3));
        assertThat(actual.get(0), is("thhe"));
        assertThat(actual.get(1), is("faaaat"));
        assertThat(actual.get(2), is("cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_multiple_words_per_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("the fat cat the fat cat", 3);
        assertThat(actual.size(), is(3));
        assertThat(actual.get(0), is("the fat"));
        assertThat(actual.get(1), is("cat the"));
        assertThat(actual.get(2), is("fat cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_null() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows(null, 3);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_emptyString() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("", 3);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_negative_rows() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("xxx", -1);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_zero_same_as_1_row() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("XXXXX", 0);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0), is("XXXXX"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_singleBigWord() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("XXXXX", 3);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0), is("XXXXX"));
    }

    @Test
    public void test_escapeHtml_generic_type_does_not_look_like_a_tag() {
        // Regression: "java.util.List<java.io.File>" embedded raw into an HTML fragment gets its
        // "<java.io.File>" read as an unrecognised tag, which Swing's HTML renderer draws as a stray bordered
        // box instead of plain text (see ComponentPropertiesPanel/DesignerCanvas Input:/Output: display).
        String actual = StudioUIUtils.escapeHtml("java.util.List<java.io.File>");
        assertThat(actual, is("java.util.List&lt;java.io.File&gt;"));
    }

    @Test
    public void test_escapeHtml_escapes_ampersand_too() {
        String actual = StudioUIUtils.escapeHtml("Tom & Jerry");
        assertThat(actual, is("Tom &amp; Jerry"));
    }

    @Test
    public void test_escapeHtml_leaves_plain_text_unchanged() {
        String actual = StudioUIUtils.escapeHtml("java.lang.String");
        assertThat(actual, is("java.lang.String"));
    }

    @Test
    public void test_escapeHtml_null_returns_null() {
        assertThat(StudioUIUtils.escapeHtml(null), is((String) null));
    }

    @Test
    public void test_buildInputOutputSummaryHtml_returns_null_when_both_are_null() {
        assertThat(StudioUIUtils.buildInputOutputSummaryHtml(null, null, false), is((String) null));
    }

    @Test
    public void test_buildInputOutputSummaryHtml_omits_the_input_line_when_input_is_null() {
        String actual = StudioUIUtils.buildInputOutputSummaryHtml(null, "java.lang.String", false);
        assertThat(actual.contains("Input:"), is(false));
        assertThat(actual.contains("<b>Output:</b> java.lang.String"), is(true));
    }

    @Test
    public void test_buildInputOutputSummaryHtml_omits_the_output_line_when_output_is_null() {
        String actual = StudioUIUtils.buildInputOutputSummaryHtml("java.lang.String", null, false);
        assertThat(actual.contains("Output:"), is(false));
        assertThat(actual.contains("<b>Input:</b> java.lang.String"), is(true));
    }

    @Test
    public void test_buildInputOutputSummaryHtml_escapes_generic_types_in_both_halves() {
        String actual = StudioUIUtils.buildInputOutputSummaryHtml("java.util.List<String>", "java.io.File", false);
        assertThat(actual.contains("java.util.List&lt;String&gt;"), is(true));
        assertThat(actual.contains("<java.io.File>"), is(false));
    }

    @Test
    public void test_buildInputOutputSummaryHtml_appends_a_default_qualifier_only_when_isPreview() {
        String live = StudioUIUtils.buildInputOutputSummaryHtml("java.lang.String", null, false);
        String preview = StudioUIUtils.buildInputOutputSummaryHtml("java.lang.String", null, true);
        assertThat(live.contains("default"), is(false));
        assertThat(preview.contains("default"), is(true));
    }

    @Test
    public void test_buildComponentSummaryHtml_puts_the_name_first_sourced_from_the_parameter_not_helpText() {
        String actual = StudioUIUtils.buildComponentSummaryHtml("Local File Consumer", null, null, "java.util.List<java.io.File>", false);
        int nameIndex = actual.indexOf("Local File Consumer");
        int outputIndex = actual.indexOf("Output:");
        assertThat(nameIndex >= 0, is(true));
        assertThat(outputIndex > nameIndex, is(true));
    }

    @Test
    public void test_buildComponentSummaryHtml_still_shows_the_name_when_there_is_no_input_or_output() {
        String actual = StudioUIUtils.buildComponentSummaryHtml("Generic Consumer", null, null, null, false);
        assertThat(actual.contains("Generic Consumer"), is(true));
        assertThat(actual.contains("Input:"), is(false));
        assertThat(actual.contains("Output:"), is(false));
    }

    @Test
    public void test_buildComponentSummaryHtml_escapes_the_name_too() {
        String actual = StudioUIUtils.buildComponentSummaryHtml("Tom & Jerry", null, null, null, false);
        assertThat(actual.contains("Tom &amp; Jerry"), is(true));
    }

    @Test
    public void test_buildComponentSummaryHtml_shows_the_ikasan_class_simple_name_next_to_the_component_name_when_given() {
        String actual = StudioUIUtils.buildComponentSummaryHtml("Default List Splitter",
                "org.ikasan.component.splitter.DefaultListSplitter", null, null, false);
        assertThat(actual.contains("(Ikasan class: DefaultListSplitter)"), is(true));
        assertThat(actual.contains("org.ikasan.component.splitter"), is(false));
    }

    @Test
    public void test_buildComponentSummaryHtml_omits_the_ikasan_class_annotation_when_not_given() {
        String actual = StudioUIUtils.buildComponentSummaryHtml("Converter", null, null, null, false);
        assertThat(actual.contains("Ikasan class"), is(false));
    }

    @Test
    public void test_buildMoreInfoLinkHtml_returns_null_when_blank() {
        assertThat(StudioUIUtils.buildMoreInfoLinkHtml(null), is((String) null));
        assertThat(StudioUIUtils.buildMoreInfoLinkHtml(""), is((String) null));
    }

    @Test
    public void test_buildMoreInfoLinkHtml_renders_a_link_to_the_given_url() {
        String actual = StudioUIUtils.buildMoreInfoLinkHtml("https://example.com/help");
        assertThat(actual.contains("href=\"https://example.com/help\""), is(true));
        assertThat(actual.contains("More info"), is(true));
    }

}