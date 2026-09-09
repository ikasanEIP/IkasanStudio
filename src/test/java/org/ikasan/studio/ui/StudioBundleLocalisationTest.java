package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards Studio-added user-facing text that must exist in both the English and Japanese bundles. */
class StudioBundleLocalisationTest {
    private static final String ENGLISH = "messages/studioBundle.properties";
    private static final String JAPANESE = "messages/studioBundle_ja.properties";

    private static Properties load(String resource) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = StudioBundleLocalisationTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing classpath resource " + resource);
            properties.load(in);
        }
        return properties;
    }

    @Test
    void updateCodeRestartWarningIsLocalised() throws Exception {
        Properties english = load(ENGLISH);
        Properties japanese = load(JAPANESE);
        String key = "message.ModuleMustBeRestartedAfterUpdateCode";

        assertEquals("You will need to restart the module for these changes to take effect.",
                english.getProperty(key));
        assertLocalised(japanese, key);
    }

    @Test
    void triggerScanNowTransparencyTextIsLocalised() throws Exception {
        Properties english = load(ENGLISH);
        Properties japanese = load(JAPANESE);

        assertEquals("Trigger scan now limitations...", english.getProperty("menu.TriggerNowLimitations"));
        assertTrue(english.getProperty("message.TriggerNowLimitations").contains("Duplicate detection"));
        assertTrue(english.getProperty("message.ScheduledConsumerTriggeredWithCriteria").contains("{1}"));

        for (String key : List.of(
                "menu.TriggerNowLimitations",
                "message.TriggerNowLimitations",
                "message.ScheduledConsumerTriggeredWithCriteria",
                "message.ScanCriteriaUnavailable")) {
            assertLocalised(japanese, key);
        }
    }

    @Test
    void moduleRestartRequiredWarningIsLocalised() throws Exception {
        Properties english = load(ENGLISH);
        Properties japanese = load(JAPANESE);

        assertEquals("Restart required...", english.getProperty("menu.ModuleRestartRequired"));
        assertTrue(english.getProperty("message.ModuleRestartRequired").contains("Restart the module"));
        assertTrue(english.getProperty("tooltip.ModuleRestartRequiredForChange").contains("Restart the module"));

        for (String key : List.of(
                "menu.ModuleRestartRequired",
                "message.ModuleRestartRequired",
                "tooltip.ModuleRestartRequiredForChange")) {
            assertLocalised(japanese, key);
        }
    }

    @Test
    void modelJsonImportTextIsLocalised() throws Exception {
        Properties english = load(ENGLISH);
        Properties japanese = load(JAPANESE);

        assertEquals("Import model.json...", english.getProperty("button.ImportModelJson"));
        assertEquals("Choose file...", english.getProperty("button.ChooseModelJsonFile"));
        assertTrue(english.getProperty("message.ImportModelJsonExplanation").contains("Paste a model.json"));
        assertTrue(english.getProperty("message.ImportModelSuccess").contains("{0}"));
        assertTrue(english.getProperty("message.ImportModelJdkNotConfigured").contains("Project Structure"));
        assertTrue(english.getProperty("message.LoadJsonModelFromFile").contains("model.json"));

        for (String key : List.of(
                "button.ImportModelJson",
                "button.ChooseModelJsonFile",
                "dialog.ImportModelJson",
                "dialog.ChooseModelJsonFile",
                "message.ImportModelJsonExplanation",
                "message.ChooseModelJsonFileDescription",
                "message.ImportModelNoContent",
                "message.ImportModelFileReadFailed",
                "message.ImportModelFailed",
                "message.ImportModelMetapackUnavailable",
                "message.ImportModelSuccess",
                "message.ImportModelJdkNotConfigured",
                "message.LoadJsonModelFromFile")) {
            assertLocalised(japanese, key);
        }
    }

    private static void assertLocalised(Properties japanese, String key) {
        assertTrue(japanese.containsKey(key), "Japanese bundle must define " + key);
        assertFalse(japanese.getProperty(key).isBlank(), "Japanese translation must not be blank for " + key);
    }
}
