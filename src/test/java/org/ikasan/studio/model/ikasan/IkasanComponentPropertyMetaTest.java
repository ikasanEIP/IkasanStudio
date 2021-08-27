package org.ikasan.studio.model.ikasan;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class IkasanComponentPropertyMetaTest {
    IkasanComponentPropertyMeta ikasanComponentPropertyMeta;
//    public static final String CLASS_NAME_PATTERN = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    public static final String CLASS_NAME_PATTERN = "[A-Z_$][a-zA-Z\\d_$]*";

    @Before
    public void setup() {
        ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(
                1, 1, true, true, true, true,
                "Test property name", "config_label", java.lang.String.class,
                "java.lang.String",CLASS_NAME_PATTERN, "defaultValue", "some help");
    }

    @Test
    public void test_pattern_matcher_is_populated_and_works() {
        Pattern validationPattern = ikasanComponentPropertyMeta.getValidationPattern();
        assertNotNull(validationPattern);
        {
            Matcher matcher = validationPattern.matcher("invalid class");
            assertThat(matcher.matches(), is(false));
        }
        {
            Matcher matcher = validationPattern.matcher("ValidClass");
            assertThat(matcher.matches(), is(true));
        }
        {
            Matcher matcher = validationPattern.matcher("2InValidClass");
            assertThat(matcher.matches(), is(false));
        }
        {
            Matcher matcher = validationPattern.matcher("inValidClass");
            assertThat(matcher.matches(), is(false));
        }

    }

}