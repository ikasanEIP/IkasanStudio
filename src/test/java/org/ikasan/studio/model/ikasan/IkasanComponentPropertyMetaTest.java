package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IkasanComponentPropertyMetaTest {
    IkasanComponentPropertyMeta ikasanComponentPropertyMeta;
//    public static final String CLASS_NAME_PATTERN = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    public static final String CLASS_NAME_PATTERN = "[A-Z_$][a-zA-Z\\d_$]*";

    @BeforeEach
    public void setup() {
        ikasanComponentPropertyMeta = IkasanComponentPropertyMeta.builder()
                .propertyName("1")
                .build();
//        ikasanComponentPropertyMeta = new IkasanComponentPropertyMeta(
//                1, false, true, true, true, true,
//                "Test property name", "config_label", java.lang.String.class,
//                "java.lang.String",CLASS_NAME_PATTERN, "Please enter a valid clnassname", "defaultValue", "some help");
    }

    @Test
    @Disabled
    public void test_pattern_matcher_is_populated_and_works() {
        Pattern validationPattern = ikasanComponentPropertyMeta.getValidationPattern();
        Assertions.assertNotNull(validationPattern);
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