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

}