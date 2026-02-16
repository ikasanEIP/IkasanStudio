package org.ikasan.studio.ui.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for StudioPsiUtils.getJsonAttribute() method.
 * Tests cover:
 * - Valid JSON with existing attributes
 * - Valid JSON with missing attributes
 * - Null and empty input handling
 * - Malformed JSON handling
 * - Null attribute values in JSON
 * - Edge cases and boundary conditions
 */
public class StudioPsiUtilsJsonAttributeTest {

    // ============================================================================
    // POSITIVE TEST CASES - Valid JSON and successful extractions
    // ============================================================================

    @Test
    public void testExtractSimpleStringAttribute() {
        // Test extracting a simple string attribute from valid JSON
        String json = "{\"name\":\"untitled54\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractApplicationPackageNameAttribute() {
        // Test the primary use case - extracting applicationPackageName
        String json = "{\"name\":\"MyModule\",\"applicationPackageName\":\"com.company.app\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("com.company.app", result);
    }

    @Test
    public void testExtractNameAttribute() {
        // Test extracting name attribute
        String json = "{\"name\":\"MyModule\",\"applicationPackageName\":\"com.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "name");

        assertEquals("MyModule", result);
    }

    @Test
    public void testExtractAttributeFromJsonWithMultipleAttributes() {
        // Test extracting from JSON with many attributes
        String json = "{\"name\":\"test\",\"version\":\"1.0\",\"applicationPackageName\":\"org.example\",\"port\":\"8080\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractAttributeWithSpecialCharacters() {
        // Test extracting attribute value with special characters
        String json = "{\"path\":\"/home/user/project\",\"applicationPackageName\":\"org.example.sub-package\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example.sub-package", result);
    }

    @Test
    public void testExtractAttributeWithNumericValue() {
        // Test extracting numeric value (returned as string)
        String json = "{\"port\":\"8080\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "port");

        assertEquals("8080", result);
    }

    @Test
    public void testExtractAttributeWithEmptyStringValue() {
        // Test extracting attribute with empty string value
        String json = "{\"name\":\"\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "name");

        assertEquals("", result);
    }

    @Test
    public void testExtractAttributeFromMinimalJson() {
        // Test with minimal valid JSON
        String json = "{\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractAttributeFromCompactJson() {
        // Test with compact JSON (no spaces)
        String json = "{\"name\":\"test\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractAttributeFromFormattedJson() {
        // Test with formatted JSON (multiple lines, indentation)
        String json = "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"applicationPackageName\": \"org.example\"\n" +
                "}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    // ============================================================================
    // NEGATIVE TEST CASES - Missing attributes and non-existent values
    // ============================================================================

    @Test
    public void testExtractNonExistentAttribute() {
        // Test that method returns null for non-existent attribute
        String json = "{\"name\":\"test\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "nonExistent");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromEmptyJson() {
        // Test with empty JSON object
        String json = "{}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithWrongCasing() {
        // Test that attribute names are case-sensitive
        String json = "{\"ApplicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    // ============================================================================
    // NULL INPUT TEST CASES
    // ============================================================================

    @Test
    public void testExtractAttributeWithNullJsonString() {
        // Test that null JSON string returns null
        String result = StudioPsiUtils.getJsonAttribute(null, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithNullAttributeName() {
        // Test that null attribute name returns null
        String json = "{\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, null);

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithBothParametersNull() {
        // Test that both null parameters return null
        String result = StudioPsiUtils.getJsonAttribute(null, null);

        assertNull(result);
    }

    // ============================================================================
    // EMPTY STRING INPUT TEST CASES
    // ============================================================================

    @Test
    public void testExtractAttributeWithEmptyJsonString() {
        // Test that empty JSON string returns null
        String result = StudioPsiUtils.getJsonAttribute("", "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithWhitespaceOnlyJsonString() {
        // Test that whitespace-only JSON string returns null
        String result = StudioPsiUtils.getJsonAttribute("   ", "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithEmptyAttributeName() {
        // Test that empty attribute name returns null
        String json = "{\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithWhitespaceOnlyAttributeName() {
        // Test that whitespace-only attribute name returns null
        String json = "{\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "   ");

        assertNull(result);
    }

    // ============================================================================
    // MALFORMED JSON TEST CASES
    // ============================================================================

    @Test
    public void testExtractAttributeFromInvalidJson() {
        // Test that invalid JSON returns null
        String json = "{invalid json}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromJsonMissingClosingBrace() {
        // Test malformed JSON missing closing brace
        String json = "{\"applicationPackageName\":\"org.example\"";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromJsonWithTrailingComma() {
        // Test JSON with trailing comma (invalid)
        String json = "{\"name\":\"test\",\"applicationPackageName\":\"org.example\",}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromJsonUnquotedKeys() {
        // Test JSON with unquoted keys (invalid JSON)
        String json = "{name:\"test\",applicationPackageName:\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromNotJsonString() {
        // Test with plain text that's not JSON
        String notJson = "This is not JSON";
        String result = StudioPsiUtils.getJsonAttribute(notJson, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeFromXmlString() {
        // Test with XML string instead of JSON
        String xml = "<root><applicationPackageName>org.example</applicationPackageName></root>";
        String result = StudioPsiUtils.getJsonAttribute(xml, "applicationPackageName");

        assertNull(result);
    }

    // ============================================================================
    // NULL ATTRIBUTE VALUE IN JSON TEST CASES
    // ============================================================================

    @Test
    public void testExtractAttributeWithNullValueInJson() {
        // Test extracting attribute that has null value in JSON
        String json = "{\"name\":\"test\",\"applicationPackageName\":null}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    @Test
    public void testExtractAttributeWithNullValueAmongOtherAttributes() {
        // Test extracting null attribute when other attributes exist
        String json = "{\"name\":\"test\",\"applicationPackageName\":null,\"version\":\"1.0\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertNull(result);
    }

    // ============================================================================
    // EDGE CASES AND BOUNDARY CONDITIONS
    // ============================================================================

    @Test
    public void testExtractAttributeWithVeryLongValue() {
        // Test extracting very long attribute value
        String longValue = "a".repeat(10000);
        String json = "{\"data\":\"" + longValue + "\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "data");

        assertEquals(longValue, result);
    }

    @Test
    public void testExtractAttributeWithUnicodeCharacters() {
        // Test extracting attribute with Unicode characters
        String json = "{\"name\":\"测试\",\"applicationPackageName\":\"org.例え\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.例え", result);
    }

    @Test
    public void testExtractAttributeWithEscapedQuotes() {
        // Test extracting attribute value with escaped quotes
        String json = "{\"message\":\"He said \\\"hello\\\"\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "message");

        assertEquals("He said \"hello\"", result);
    }

    @Test
    public void testExtractAttributeFromNestedJson() {
        // Test that nested JSON objects don't interfere
        String json = "{\"nested\":{\"inner\":\"value\"},\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractAttributeFromJsonWithArrays() {
        // Test that JSON arrays don't interfere with attribute extraction
        String json = "{\"items\":[\"a\",\"b\"],\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("org.example", result);
    }

    @Test
    public void testExtractAttributeWithDotsInValue() {
        // Test extracting package name with dots (common pattern)
        String json = "{\"applicationPackageName\":\"com.example.company.app.module\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("com.example.company.app.module", result);
    }

    @Test
    public void testExtractAttributeWithHyphensInValue() {
        // Test extracting value with hyphens
        String json = "{\"module-name\":\"my-module-v2\",\"applicationPackageName\":\"org.example\"}";
        String result = StudioPsiUtils.getJsonAttribute(json, "module-name");

        assertEquals("my-module-v2", result);
    }

    // ============================================================================
    // REAL-WORLD USAGE TEST CASES
    // ============================================================================

    @Test
    public void testRealWorldModuleJson() {
        // Test with realistic module JSON structure
        String json = "{\n" +
                "  \"name\" : \"MyIntegrationModule\",\n" +
                "  \"applicationPackageName\" : \"com.example.integration\",\n" +
                "  \"version\" : \"1.0.0\",\n" +
                "  \"port\" : \"8080\"\n" +
                "}";

        String name = StudioPsiUtils.getJsonAttribute(json, "name");
        String packageName = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");
        String version = StudioPsiUtils.getJsonAttribute(json, "version");
        String port = StudioPsiUtils.getJsonAttribute(json, "port");

        assertEquals("MyIntegrationModule", name);
        assertEquals("com.example.integration", packageName);
        assertEquals("1.0.0", version);
        assertEquals("8080", port);
    }

    @Test
    public void testRealWorldMinimalModuleJson() {
        // Test with minimal module JSON (as in the original requirement)
        String json = "{\n" +
                "  \"name\" : \"untitled54\",\n" +
                "  \"applicationPackageName\" : \"org.example\"\n" +
                "}";

        String name = StudioPsiUtils.getJsonAttribute(json, "name");
        String packageName = StudioPsiUtils.getJsonAttribute(json, "applicationPackageName");

        assertEquals("untitled54", name);
        assertEquals("org.example", packageName);
    }

}

