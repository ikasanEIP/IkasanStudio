package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversionRecipeEditorTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void showsOnlyApplicableSettingsAndPreservesHiddenDrafts(String pack) throws Exception {
        var converter = TestFixtures.getCustomConverter(pack);
        SwingUtilities.invokeAndWait(() -> {
            Map<String, ComponentPropertyEditRow> rows = new HashMap<>();
            for (String key : List.of("conversionRecipeId", "fromType", "toType", "recipeCharset",
                    "recipeFilename", "recipeMediaType", "recipeEmailBody")) {
                rows.put(key, new ComponentPropertyEditRow(null,
                        new ComponentProperty(converter.getComponentMeta().getMetadata(key), converter.getPropertyValue(key)), false, () -> {}, rows));
            }
            ConversionRecipeEditor.bind(converter.getComponentMeta().getConversionRecipes(), rows, new JTextArea(), null, null);
            var choice = rows.get("conversionRecipeId").getInputField().getPropertyChoiceValueField();
            choice.setSelectedItem("jms-message-to-email-attachment");
            var media = rows.get("recipeMediaType");
            media.getInputField().getPropertyValueField().setValue("image/png");
            assertTrue(media.getPropertyTitleField().isVisible());
            choice.setSelectedItem("jms-message-to-file-transfer-payload");
            assertTrue(rows.get("recipeCharset").getPropertyTitleField().isVisible());
            assertTrue(rows.get("recipeFilename").getPropertyTitleField().isVisible());
            assertFalse(media.getPropertyTitleField().isVisible());
            assertFalse(media.getInputField().getFirstFocusComponent().isVisible());
            assertFalse(rows.get("recipeEmailBody").getPropertyTitleField().isVisible());
            media.setRowVisible(true); // Clearing a search must not reveal irrelevant fields.
            assertFalse(media.getPropertyTitleField().isVisible());
            media.setRowVisible(false);
            choice.setSelectedItem("jms-message-to-email-attachment");
            assertFalse(media.getPropertyTitleField().isVisible());
            media.setRowVisible(true);
            assertTrue(media.getPropertyTitleField().isVisible());
            assertEquals("image/png", media.getValue());
        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void defaultsOnlyAnUnambiguousNewDraft(String pack) throws Exception {
        var converter = TestFixtures.getCustomConverter(pack);
        SwingUtilities.invokeAndWait(() -> {
            assertDefault(converter, true, null, "java.lang.String", "org.ikasan.filetransfer.Payload", "string-to-file-transfer-payload");
            assertDefault(converter, false, null, "java.lang.String", "org.ikasan.filetransfer.Payload", null);
            assertDefault(converter, true, null, "java.lang.String", "org.ikasan.component.endpoint.email.producer.EmailPayload", null);
            assertDefault(converter, true, null, "com.acme.Unknown", "org.ikasan.filetransfer.Payload", null);
            assertDefault(converter, true, "removed-recipe", "java.lang.String", "org.ikasan.filetransfer.Payload", "removed-recipe");
            assertNull(converter.getPropertyValue("conversionRecipeId"));
        });
    }

    private void assertDefault(org.ikasan.studio.core.model.ikasan.instance.FlowElement converter,
                               boolean initialising, String saved, String source, String target, String expected) {
        Map<String, ComponentPropertyEditRow> rows = new HashMap<>();
        rows.put("conversionRecipeId", new ComponentPropertyEditRow(null,
                new ComponentProperty(converter.getComponentMeta().getMetadata("conversionRecipeId"), saved), false));
        rows.put("fromType", new ComponentPropertyEditRow(null,
                new ComponentProperty(converter.getComponentMeta().getMetadata("fromType"), source), false));
        rows.put("toType", new ComponentPropertyEditRow(null,
                new ComponentProperty(converter.getComponentMeta().getMetadata("toType"), target), false));
        ConversionRecipeEditor.bind(converter.getComponentMeta().getConversionRecipes(), rows,
                new JTextArea(), null, null, initialising);
        assertEquals(expected, rows.get("conversionRecipeId").getValue());
        assertEquals(saved, rows.get("conversionRecipeId").getComponentProperty().getValue());
        assertEquals(!Objects.equals(saved, expected), rows.get("conversionRecipeId").propertyValueHasChanged());
        if (initialising && saved == null && expected != null) {
            rows.get("conversionRecipeId").getInputField().getPropertyChoiceValueField().setSelectedItem("");
            assertNull(rows.get("conversionRecipeId").getValue());
        }
    }

    @Test void openingAnUnavailableSavedRecipePreservesIt() throws Exception {
        var converter = TestFixtures.getCustomConverter("V4.1.6");
        SwingUtilities.invokeAndWait(() -> {
            Map<String, ComponentPropertyEditRow> rows = new HashMap<>();
            rows.put("conversionRecipeId", new ComponentPropertyEditRow(null,
                    new ComponentProperty(converter.getComponentMeta().getMetadata("conversionRecipeId"), "removed-recipe"), false));
            ConversionRecipeEditor.bind(converter.getComponentMeta().getConversionRecipes(), rows, new JTextArea(), null, null);
            assertEquals("removed-recipe", rows.get("conversionRecipeId").getValue());
            assertFalse(rows.get("conversionRecipeId").propertyValueHasChanged());
            assertFalse(ConversionRecipeEditor.validate(converter.getComponentMeta().getConversionRecipes(), rows).isEmpty());
        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void selectionChangesOnlyDraftAndSwitchingTypesRequiresReselection(String pack) throws Exception {
        var converter = TestFixtures.getCustomConverter(pack);
        SwingUtilities.invokeAndWait(() -> {
            Map<String, ComponentPropertyEditRow> rows = new HashMap<>();
            for (String key : List.of("conversionRecipeId", "fromType", "toType", "recipeCharset")) {
                var property = new ComponentProperty(converter.getComponentMeta().getMetadata(key), converter.getPropertyValue(key));
                rows.put(key, new ComponentPropertyEditRow(null, property, false));
            }
            var details = new JTextArea();
            ConversionRecipeEditor.bind(converter.getComponentMeta().getConversionRecipes(), rows, details, null, null);
            var combo = rows.get("conversionRecipeId").getInputField().getPropertyChoiceValueField();
            combo.setSelectedItem("string-to-email-attachment");
            assertEquals("java.lang.String", rows.get("fromType").getValue());
            assertEquals("org.ikasan.component.endpoint.email.producer.EmailPayload", rows.get("toType").getValue());
            assertNull(converter.getPropertyValue("conversionRecipeId"));
            assertTrue(rows.get("conversionRecipeId").propertyValueHasChanged());
            assertTrue(rows.get("conversionRecipeId").getMeta().isAffectsUserImplementedClass());
            assertTrue(details.getText().contains("attachment"));
            assertTrue(ConversionRecipeEditor.validate(converter.getComponentMeta().getConversionRecipes(), rows).isEmpty());
            rows.get("toType").getInputField().getPropertyValueField().setValue("byte[]");
            assertFalse(ConversionRecipeEditor.validate(converter.getComponentMeta().getConversionRecipes(), rows).isEmpty());
            for (var recipe : converter.getComponentMeta().getConversionRecipes()) {
                combo.setSelectedItem(recipe.getId());
                assertTrue(rows.get("fromType").doValidateAll().isEmpty(), recipe.getId());
                assertTrue(rows.get("toType").doValidateAll().isEmpty(), recipe.getId());
                assertTrue(ConversionRecipeEditor.validate(converter.getComponentMeta().getConversionRecipes(), rows).isEmpty(), recipe.getId());
            }
            combo.setSelectedItem("");
            assertTrue(ConversionRecipeEditor.validate(converter.getComponentMeta().getConversionRecipes(), rows).isEmpty());
        });
    }
}
