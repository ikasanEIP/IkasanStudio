package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
public class ComponentPropertyMetaTest {
    ComponentPropertyMeta componentPropertyMeta;
    public static final String CLASS_NAME_PATTERN = "^[A-Z_$][a-zA-Z\\d_$£]*$";
    public static final String VALIDATION_MESSAGE = "Please provide a class name";

    @BeforeEach
    public void setup() {
        componentPropertyMeta = ComponentPropertyMeta.builder()
                .propertyName("1")
                .validation(CLASS_NAME_PATTERN)
                .validationMessage(VALIDATION_MESSAGE)
                .build();
    }

    @Test
    public void test_pattern_matcher_is_populated_and_works() {
        Pattern validationPattern = componentPropertyMeta.getValidationPattern();
        Assertions.assertNotNull(validationPattern);
        {
            Matcher matcher = validationPattern.matcher("invalid class");
            assertAll(
                    "Check the module contains the expected values",
                    () -> assertFalse(matcher.matches()),
                    () -> assertEquals(VALIDATION_MESSAGE, componentPropertyMeta.getValidationMessage())
            );
        }
        {
            Matcher matcher = validationPattern.matcher("ValidClass");
            assertAll(
                    "Check the module contains the expected values",
                    () -> assertTrue(matcher.matches()),
                    () -> assertEquals(VALIDATION_MESSAGE, componentPropertyMeta.getValidationMessage())
            );
        }
        {
            Matcher matcher = validationPattern.matcher("2InValidClass");
            assertAll(
                    "Check the module contains the expected values",
                    () -> assertFalse(matcher.matches()),
                    () -> assertEquals(VALIDATION_MESSAGE, componentPropertyMeta.getValidationMessage())
            );
        }
        {
            Matcher matcher = validationPattern.matcher("inValidClass");
            assertAll(
                    "Check the module contains the expected values",
                    () -> assertFalse(matcher.matches()),
                    () -> assertEquals(VALIDATION_MESSAGE, componentPropertyMeta.getValidationMessage())
            );
        }

    }

}