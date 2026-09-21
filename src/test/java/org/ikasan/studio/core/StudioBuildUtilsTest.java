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
    void configurationConversionDoesNotPrintCredentials() {
        java.io.PrintStream original = System.out;
        var output = new java.io.ByteArrayOutputStream();
        try (var capture = new java.io.PrintStream(output)) {
            System.setOut(capture);
            Map<String, String> properties = StudioBuildUtils.convertStringToMap("password=CANARY_SECRET\ncopy=${password}");
            assertEquals("CANARY_SECRET", properties.get("copy"));
            assertEquals("", output.toString());
        } finally { System.setOut(original); }
    }

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
    public void toJavaTypeLiteral_strips_a_trailing_explanatory_annotation_like_the_upstream_output_description_carries() {
        // Regression test: a fromType/toType property is free text, and a user can easily copy the display text
        // shown for an upstream component's Output: (e.g. a JMS consumer with Auto Content Conversion on reads
        // "java.lang.Object (auto-converted)" - see ComponentMeta#getEffectiveOutputTypeDescription) verbatim into
        // a Converter's fromType field, which would otherwise generate an uncompilable Java type literal.
        assertThat(StudioBuildUtils.toJavaTypeLiteral("java.lang.Object (auto-converted)"), is("java.lang.Object"));
        assertThat(StudioBuildUtils.toJavaTypeLiteral("java.lang.String"), is("java.lang.String"));
        assertThat(StudioBuildUtils.toJavaTypeLiteral("  java.lang.String  "), is("java.lang.String"));
        assertThat(StudioBuildUtils.toJavaTypeLiteral(null), is((String) null));
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

    @Test
    void stringToListKeepsUserOrderWhenRemovingDuplicates() {
        assertThat(StudioBuildUtils.stringToList("zebra, apple, mango, apple, banana, zebra"),
                is(List.of("zebra", "apple", "mango", "banana")));
        assertThat(StudioBuildUtils.stringToList("[c, b, a]"), is(List.of("c", "b", "a")));
    }

    /**
     * Spring Boot reads .properties files as ISO-8859-1 but Studio writes them as UTF-8, so a raw non-ASCII
     * character (an accented directory, a Japanese name) arrives at runtime as mojibake. Escaping it as a
     * \\uXXXX sequence is decoded correctly by both Properties and Spring Boot.
     */
    @Test
    public void testEscapeSpringPropertiesValue_escapesNonAsciiSoItSurvivesAnIso88591Reader() throws IOException {
        String original = "/donn\u00e9es/\u65e5\u672c\u8a9e/\u20ac \ud83d\ude00";
        String escaped = StudioBuildUtils.escapeSpringPropertiesValue(original);

        assertThat("only ASCII may remain, so the file reads the same in any charset",
                escaped.chars().allMatch(c -> c < 0x7f), is(true));
        java.util.Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader("dir=" + escaped));
        assertThat(properties.getProperty("dir"), is(original));
        assertThat(StudioBuildUtils.escapeSpringPropertiesValue("plain /value with spaces"), is("plain /value with spaces"));
    }

    @Test
    public void testEscapeSpringPropertiesMapKey_escapesNonAsciiAndSpaces() throws IOException {
        String original = "Caf\u00e9 \u65e5\u672c Flow";
        String escaped = StudioBuildUtils.escapeSpringPropertiesMapKey(original);

        assertThat(escaped.chars().allMatch(c -> c < 0x7f), is(true));
        java.util.Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(escaped + "=v"));
        assertThat(properties.getProperty(original), is("v"));
    }

    /**
     * The module's servlet context path is built from this by both the generated properties file and Studio's own
     * runtime clients. A non-ASCII path is misread by Spring Boot and cannot be relied on in a URL, so fold to ASCII.
     */
    @Test
    public void testToUrlString_isAsciiSoTheGeneratedAndClientPathsAgree() {
        assertThat(StudioBuildUtils.toUrlString("My Module - One"), is("my-module-one"));
        assertThat(StudioBuildUtils.toUrlString("Caf\u00e9 Orders"), is("cafe-orders"));
        assertThat(StudioBuildUtils.toUrlString("\u65e5\u672c\u8a9e"), is("module"));
        assertThat(StudioBuildUtils.toUrlString(""), is(""));
        assertThat(StudioBuildUtils.toUrlString(null), org.hamcrest.CoreMatchers.nullValue());
    }

    /**
     * The context path is registered by Tomcat as a JMX object name, where a quote, comma, equals, colon, asterisk or
     * question mark is illegal: a module named with one generated an application that failed to start
     * (MalformedObjectNameException). Other characters below break the URLs Studio builds to reach the module.
     * Characters that are valid in both places are kept, so paths that work today do not change.
     */
    @Test
    public void testToUrlString_replacesCharactersThatBreakStartupOrTheModuleUrl() {
        assertThat(StudioBuildUtils.toUrlString("Payments: EU, phase=2"), is("payments-eu-phase-2"));
        assertThat(StudioBuildUtils.toUrlString("Orders #2"), is("orders-2"));
        assertThat(StudioBuildUtils.toUrlString("100% what?"), is("100-what-"));
        assertThat(StudioBuildUtils.toUrlString("Caf\u00e9 \"Orders\" \\ \u65e5\u672c"), is("cafe-orders-"));
        assertThat(StudioBuildUtils.toUrlString("a<b>|c"), is("a-b-c"));
        // Valid in a URL path and in a JMX name, so unchanged.
        assertThat(StudioBuildUtils.toUrlString("R&D (EU) Team's +1 @home"), is("r&d-(eu)-team's-+1-@home"));
        assertThat(StudioBuildUtils.toUrlString("My Module - One"), is("my-module-one"));
    }
}
