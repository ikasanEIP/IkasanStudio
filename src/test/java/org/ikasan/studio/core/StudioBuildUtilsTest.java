package org.ikasan.studio.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudioBuildUtilsTest {
    @Test
    public void test_get_directories() throws URISyntaxException, IOException {
        String[] expectedDirs = new String[]{"studio/metapack/Vtest.x/library/Consumer, studio/metapack/Vtest.x/library/Converter, studio/metapack/Vtest.x/library/ExceptionResolver, studio/metapack/Vtest.x/library/Flow, studio/metapack/Vtest.x/library/Module, studio/metapack/Vtest.x/library/Producer"};
        String[] actualDirs = StudioBuildUtils.getDirectories("studio/metapack/Vtest.x/library");
        Set<String> expectedDirsSorted = new TreeSet<>(List.of(expectedDirs)) ;
        Set<String> actualDirsSorted = new TreeSet<>(List.of(actualDirs)) ;

        assertAll(
                "Check the module contains the expected values",
                () -> assertEquals(6, actualDirs.length),
                () -> assertEquals(expectedDirsSorted.toString(), actualDirsSorted.toString())
        );
    }
    @Test
    public void test_get_last_token_with_multiple_tokens() {
        String actual = StudioBuildUtils.getLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("delim"));

        String actual2 = StudioBuildUtils.getLastToken("\\.", "delim");
        assertThat(actual2, is("delim"));
    }

    @Test
    public void test_get_all_but_last_token_with_multiple_tokens() {
        String actual = StudioBuildUtils.getAllButLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("this.is.dot"));

        String actual2 = StudioBuildUtils.getAllButLastToken("\\.", "this.delim");
        assertThat(actual2, is("this"));

        String actual3 = StudioBuildUtils.getAllButLastToken("\\.", "delim");
        assertThat(actual3, is(""));
    }

    @Test
    public void toJavaClassName() {
        assertThat(StudioBuildUtils.toJavaClassName(""), is(""));
        assertThat(StudioBuildUtils.toJavaClassName("a"), is("A"));
        assertThat(StudioBuildUtils.toJavaClassName("A"), is("A"));
        assertThat(StudioBuildUtils.toJavaClassName("AS"), is("AS"));
        assertThat(StudioBuildUtils.toJavaClassName("AS D"), is("ASD"));
        assertThat(StudioBuildUtils.toJavaClassName("as d"), is("AsD"));
        assertThat(StudioBuildUtils.toJavaClassName("as d    c"), is("AsDC"));
        assertThat(StudioBuildUtils.toJavaClassName("Some Text"), is("SomeText"));
        assertThat(StudioBuildUtils.toJavaClassName("my.package.name"), is("MyPackageName"));
    }

    @Test
    public void testToJavaIdentifier() {
        assertThat(StudioBuildUtils.toJavaIdentifier(""), is(""));
        assertThat(StudioBuildUtils.toJavaIdentifier("a"), is("a"));
        assertThat(StudioBuildUtils.toJavaIdentifier("A"), is("a"));
        assertThat(StudioBuildUtils.toJavaIdentifier("AS"), is("aS"));
        assertThat(StudioBuildUtils.toJavaIdentifier("AS D"), is("aSD"));
        assertThat(StudioBuildUtils.toJavaIdentifier("as d"), is("asD"));
        assertThat(StudioBuildUtils.toJavaIdentifier("as d    c"), is("asDC"));
        assertThat(StudioBuildUtils.toJavaIdentifier("Some Text"), is("someText"));
        assertThat(StudioBuildUtils.toJavaIdentifier("my.package.name"), is("myPackageName"));
    }

    @Test
    public void testToJavaPackageName() {
        assertThat(StudioBuildUtils.toJavaPackageName(""), is(""));
        assertThat(StudioBuildUtils.toJavaPackageName("a"), is("a"));
        assertThat(StudioBuildUtils.toJavaPackageName("A"), is("a"));
        assertThat(StudioBuildUtils.toJavaPackageName("AS"), is("as"));
        assertThat(StudioBuildUtils.toJavaPackageName("AS D"), is("asd"));
        assertThat(StudioBuildUtils.toJavaPackageName("as d"), is("asd"));
        assertThat(StudioBuildUtils.toJavaPackageName("as d    c"), is("asdc"));
        assertThat(StudioBuildUtils.toJavaPackageName("Some 1 Text"), is("some1text"));
        assertThat(StudioBuildUtils.toJavaPackageName("1test"), is("_1test"));
    }

    @Test
    public void testToUrlString() {
        assertThat(StudioBuildUtils.toUrlString(""), is(""));
        assertThat(StudioBuildUtils.toUrlString("a"), is("a"));
        assertThat(StudioBuildUtils.toUrlString("A"), is("a"));
        assertThat(StudioBuildUtils.toUrlString("AS"), is("as"));
        assertThat(StudioBuildUtils.toUrlString("AS D"), is("as-d"));
        assertThat(StudioBuildUtils.toUrlString("as d"), is("as-d"));
        assertThat(StudioBuildUtils.toUrlString("as d    c"), is("as-d-c"));
        assertThat(StudioBuildUtils.toUrlString("Some 1 Text"), is("some-1-text"));
    }

    @Test
    public void testConvertStringToMap_with_valid_name_value_map() {
        String nameValuePairs = "aa=bb\ncc=dd=ee\nff=\n=cc\n#ignore=zz";
        Map<String, String> nameValueMap = StudioBuildUtils.convertStringToMap(nameValuePairs);

        assertThat(nameValueMap.size(), is(3));
        assertThat(nameValueMap.get("aa"), is("bb"));
        assertThat(nameValueMap.get("cc"), is("dd=ee"));
        assertThat(nameValueMap.get("ff"), is(""));
    }

    @Test
    public void testConvertStringToMap_with_valid_name_value_map_replaces_placeholders() {
        String nameValuePairs = "aa=bb\ncc=${aa}=ee\ndd=xyz\nff=\n=cc\ngg=${aa}-${dd}\nhh=${cat}\n#ignore=zz";
        Map<String, String> nameValueMap = StudioBuildUtils.convertStringToMap(nameValuePairs);

        assertThat(nameValueMap.size(), is(6));
        assertThat(nameValueMap.get("aa"), is("bb"));
        assertThat(nameValueMap.get("cc"), is("bb=ee"));
        assertThat(nameValueMap.get("ff"), is(""));
        assertThat(nameValueMap.get("gg"), is("bb-xyz"));
        assertThat(nameValueMap.get("hh"), is("${cat}"));
    }

    @Test
    public void testConvertStringToMap_with_empty_name_value_map() {
        assertThat(StudioBuildUtils.convertStringToMap("no_equals_signs_present").size(), is(0));
        assertThat(StudioBuildUtils.convertStringToMap("").size(), is(0));
        assertThat(StudioBuildUtils.convertStringToMap(null).size(), is(0));
    }
}