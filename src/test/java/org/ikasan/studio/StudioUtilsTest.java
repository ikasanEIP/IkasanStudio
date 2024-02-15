package org.ikasan.studio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StudioUtilsTest {
    @Test
    public void test_get_directories() throws URISyntaxException, IOException {
        String[] expectedDirs = new String[]{"studio/Vtest.x/components/CUSTOM_CONVERTER, studio/Vtest.x/components/DEV_NULL_PRODUCER, studio/Vtest.x/components/EVENT_GENERATING_CONSUMER, studio/Vtest.x/components/EXCEPTION_RESOLVER, studio/Vtest.x/components/FLOW, studio/Vtest.x/components/MODULE"};
        String[] actualDirs = StudioUtils.getDirectories("studio/Vtest.x/components");
        Set<String> expectedDirsSorted = new TreeSet<>(List.of(expectedDirs)) ;
        Set<String> actualDirsSorted = new TreeSet<>(List.of(actualDirs)) ;
        assertThat(actualDirs.length, is(6));
        assertThat(actualDirsSorted.toString(), is(expectedDirsSorted.toString()));
    }
    @Test
    public void test_get_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("delim"));

        String actual2 = StudioUtils.getLastToken("\\.", "delim");
        assertThat(actual2, is("delim"));
    }

    @Test
    public void test_get_all_but_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getAllButLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("this.is.dot"));

        String actual2 = StudioUtils.getAllButLastToken("\\.", "this.delim");
        assertThat(actual2, is("this"));

        String actual3 = StudioUtils.getAllButLastToken("\\.", "delim");
        assertThat(actual3, is(""));
    }

    @Test
    public void toJavaClassName() {
        assertThat(StudioUtils.toJavaClassName(""), is(""));
        assertThat(StudioUtils.toJavaClassName("a"), is("A"));
        assertThat(StudioUtils.toJavaClassName("A"), is("A"));
        assertThat(StudioUtils.toJavaClassName("AS"), is("AS"));
        assertThat(StudioUtils.toJavaClassName("AS D"), is("ASD"));
        assertThat(StudioUtils.toJavaClassName("as d"), is("AsD"));
        assertThat(StudioUtils.toJavaClassName("as d    c"), is("AsDC"));
        assertThat(StudioUtils.toJavaClassName("Some Text"), is("SomeText"));
        assertThat(StudioUtils.toJavaClassName("my.package.name"), is("MyPackageName"));
    }

    @Test
    public void testToJavaIdentifier() {
        assertThat(StudioUtils.toJavaIdentifier(""), is(""));
        assertThat(StudioUtils.toJavaIdentifier("a"), is("a"));
        assertThat(StudioUtils.toJavaIdentifier("A"), is("a"));
        assertThat(StudioUtils.toJavaIdentifier("AS"), is("aS"));
        assertThat(StudioUtils.toJavaIdentifier("AS D"), is("aSD"));
        assertThat(StudioUtils.toJavaIdentifier("as d"), is("asD"));
        assertThat(StudioUtils.toJavaIdentifier("as d    c"), is("asDC"));
        assertThat(StudioUtils.toJavaIdentifier("Some Text"), is("someText"));
        assertThat(StudioUtils.toJavaIdentifier("my.package.name"), is("myPackageName"));
    }

    @Test
    public void testToJavaPackageName() {
        assertThat(StudioUtils.toJavaPackageName(""), is(""));
        assertThat(StudioUtils.toJavaPackageName("a"), is("a"));
        assertThat(StudioUtils.toJavaPackageName("A"), is("a"));
        assertThat(StudioUtils.toJavaPackageName("AS"), is("as"));
        assertThat(StudioUtils.toJavaPackageName("AS D"), is("asd"));
        assertThat(StudioUtils.toJavaPackageName("as d"), is("asd"));
        assertThat(StudioUtils.toJavaPackageName("as d    c"), is("asdc"));
        assertThat(StudioUtils.toJavaPackageName("Some 1 Text"), is("some1text"));
        assertThat(StudioUtils.toJavaPackageName("1test"), is("_1test"));
    }

    @Test
    public void testToUrlString() {
        assertThat(StudioUtils.toUrlString(""), is(""));
        assertThat(StudioUtils.toUrlString("a"), is("a"));
        assertThat(StudioUtils.toUrlString("A"), is("a"));
        assertThat(StudioUtils.toUrlString("AS"), is("as"));
        assertThat(StudioUtils.toUrlString("AS D"), is("as-d"));
        assertThat(StudioUtils.toUrlString("as d"), is("as-d"));
        assertThat(StudioUtils.toUrlString("as d    c"), is("as-d-c"));
        assertThat(StudioUtils.toUrlString("Some 1 Text"), is("some-1-text"));
    }
}