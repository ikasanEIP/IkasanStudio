package org.ikasan.studio;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

public class SUtilsTest extends TestCase {

    @Test
    public void test_get_last_token_with_multiple_tokens() {
        String actual = SUtils.getLastToken("\\.", "this.is.dot.delim");
        Assert.assertThat(actual, is("delim"));

        String actual2 = SUtils.getLastToken("\\.", "delim");
        Assert.assertThat(actual2, is("delim"));
    }

    public void testToJavaIdentifier() {
        Assert.assertThat(SUtils.toJavaIdentifier(""), is(""));
        Assert.assertThat(SUtils.toJavaIdentifier("a"), is("a"));
        Assert.assertThat(SUtils.toJavaIdentifier("A"), is("a"));
        Assert.assertThat(SUtils.toJavaIdentifier("AS"), is("as"));
        Assert.assertThat(SUtils.toJavaIdentifier("AS D"), is("asD"));
        Assert.assertThat(SUtils.toJavaIdentifier("as d"), is("asD"));
        Assert.assertThat(SUtils.toJavaIdentifier("as d    c"), is("asDC"));
        Assert.assertThat(SUtils.toJavaIdentifier("Some Text"), is("someText"));
    }
}