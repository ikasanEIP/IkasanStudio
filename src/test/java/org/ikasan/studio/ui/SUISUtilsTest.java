package org.ikasan.studio.ui;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;

public class SUISUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_splitStringIntoMultipleRows_simple_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("the fat cat", 3);
        Assert.assertThat(actual.size(), is(3));
        Assert.assertThat(actual.get(0), is("the"));
        Assert.assertThat(actual.get(1), is("fat"));
        Assert.assertThat(actual.get(2), is("cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_unequal_word_length_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("thhe faaaat cat", 3);
        Assert.assertThat(actual.size(), is(3));
        Assert.assertThat(actual.get(0), is("thhe"));
        Assert.assertThat(actual.get(1), is("faaaat"));
        Assert.assertThat(actual.get(2), is("cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_multiple_words_per_split() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("the fat cat the fat cat", 3);
        Assert.assertThat(actual.size(), is(3));
        Assert.assertThat(actual.get(0), is("the fat"));
        Assert.assertThat(actual.get(1), is("cat the"));
        Assert.assertThat(actual.get(2), is("fat cat"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_null() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows(null, 3);
        Assert.assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_emptyString() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("", 3);
        Assert.assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_negative_rows() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("xxx", -1);
        Assert.assertThat(actual.size(), is(0));
    }

    @Test
    public void test_splitStringIntoMultipleRows_zero_same_as_1_row() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("XXXXX", 0);
        Assert.assertThat(actual.size(), is(1));
        Assert.assertThat(actual.get(0), is("XXXXX"));
    }

    @Test
    public void test_splitStringIntoMultipleRows_singleBigWord() {
        List<String> actual = StudioUIUtils.splitStringIntoMultipleRows("XXXXX", 3);
        Assert.assertThat(actual.size(), is(1));
        Assert.assertThat(actual.get(0), is("XXXXX"));
    }

}