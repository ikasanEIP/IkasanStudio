package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.conversion.ConversionRecipeMatcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ComposedConversionRecipeTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void everyRecipeRendersAndRetainsItsIdentity(String version) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, new ArrayList<>());
        var converter = TestFixtures.getCustomConverter(version);
        var recipes = converter.getComponentMeta().getConversionRecipes();
        assertTrue(recipes.size() >= 25);
        for (var recipe : recipes) {
            converter.setPropertyValue("conversionRecipeId", recipe.getId());
            converter.setPropertyValue("fromType", recipe.getSourceType());
            converter.setPropertyValue("toType", recipe.getTargetType());
            converter.setPropertyValue("userImplementedClassName", "Recipe" + recipes.indexOf(recipe));
            String generated = generateUserImplementedComponentTemplate(version, module, converter);
            assertTrue(generated.contains("implements Converter<" + recipe.getSourceType() + ", " + recipe.getTargetType() + ">"), recipe.getId());
            assertFalse(generated.contains("String.valueOf(source)"));
            assertFalse(generated.contains("payload.toString()"));
            assertTrue(generated.contains("throw new TransformationException"));
            String export = System.getenv("STUDIO_RECIPE_EXPORT");
            if (export != null) {
                Path directory = Path.of(export, version);
                Files.createDirectories(directory);
                Files.writeString(directory.resolve("Recipe" + recipes.indexOf(recipe) + ".java"), generated);
            }
        }
        assertEquals(2, ConversionRecipeMatcher.matching(recipes, "org.ikasan.filetransfer.Payload",
                "org.ikasan.component.endpoint.email.producer.EmailPayload").size());
        assertFalse(ConversionRecipeMatcher.matching(recipes, "java.lang.Object (auto-converted)", "org.ikasan.filetransfer.Payload").isEmpty());
        assertTrue(ConversionRecipeMatcher.matching(recipes, "com.acme.Invoice", "org.ikasan.filetransfer.Payload").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void invalidSavedRecipeFailsInsteadOfFallingBackAndConfigurationIsEscaped(String version) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, new ArrayList<>());
        var converter = TestFixtures.getCustomConverter(version);
        converter.setPropertyValue("conversionRecipeId", "not-in-this-pack");
        assertThrows(StudioGeneratorException.class, () -> generateUserImplementedComponentTemplate(version, module, converter));
        converter.setPropertyValue("conversionRecipeId", "string-to-email-attachment");
        converter.setPropertyValue("fromType", "java.lang.String");
        converter.setPropertyValue("toType", "org.ikasan.component.endpoint.email.producer.EmailPayload");
        converter.setPropertyValue("recipeFilename", "a\"b.txt");
        converter.setPropertyValue("recipeEmailBody", "line one\nline two");
        var generated = generateUserImplementedComponentTemplate(version, module, converter);
        assertTrue(generated.contains("a\\\"b.txt"));
        assertTrue(generated.contains("line one\\nline two"));
        converter.setPropertyValue("toType", "java.lang.String");
        assertThrows(StudioGeneratorException.class, () -> generateUserImplementedComponentTemplate(version, module, converter));
    }
}
