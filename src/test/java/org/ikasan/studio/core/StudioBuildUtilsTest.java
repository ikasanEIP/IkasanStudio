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
        String[] expectedDirs = new String[]{"studio/metapack/TestV1/library/ExceptionResolver, studio/metapack/TestV1/library/Flow, studio/metapack/TestV1/library/Module, studio/metapack/TestV1/library/Producer"};
        String[] actualDirs = StudioBuildUtils.getDirectories("studio/metapack/TestV1/library");
        Set<String> expectedDirsSorted = new TreeSet<>(List.of(expectedDirs)) ;
        Set<String> actualDirsSorted = new TreeSet<>(List.of(actualDirs)) ;

        assertAll(
                "Check the module contains the expected values",
                () -> assertEquals(4, actualDirs.length),
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

    /**
     * A raw backslash written unescaped into an application.properties value is silently eaten by
     * java.util.Properties' own loader on the way back in - "\." isn't a recognised escape sequence, so it just
     * drops the backslash - which is exactly what turned a user's filenamePattern regex ".*\.tmp" into ".*.tmp"
     * at runtime. escapeSpringPropertiesValue must produce output that survives a real Properties round trip.
     */
    @Test
    public void testEscapeSpringPropertiesValue_survives_a_real_properties_round_trip() throws IOException {
        String original = ".*\\.tmp";
        String escaped = StudioBuildUtils.escapeSpringPropertiesValue(original);

        java.util.Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader("filenamePattern=" + escaped));

        assertThat("the escaped form must decode back to exactly what the user typed",
                properties.getProperty("filenamePattern"), is(original));
    }

    @Test
    public void testEscapeSpringPropertiesValue_leaves_ordinary_values_and_spaces_untouched() {
        assertThat(StudioBuildUtils.escapeSpringPropertiesValue("plain value with spaces"), is("plain value with spaces"));
        assertThat(StudioBuildUtils.escapeSpringPropertiesValue(""), is(""));
        assertThat(StudioBuildUtils.escapeSpringPropertiesValue(null), is(""));
    }
}