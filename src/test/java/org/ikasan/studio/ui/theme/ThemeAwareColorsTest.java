package org.ikasan.studio.ui.theme;

import com.intellij.ui.JBColor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;

public class ThemeAwareColorsTest {

    @Before
    public void setUp() {
        // Clear any UIManager keys used by ThemeAwareColors to avoid test pollution
        UIManager.put("Component.borderColor", null);
        UIManager.put("Separator.separatorColor", null);
        UIManager.put("Notifications.errorForeground", null);
        UIManager.put("Notifications.warningForeground", null);
        UIManager.put("Component.focusColor", null);
    }

    @After
    public void tearDown() {
        setUp();
    }

    @Test
    public void urgentColor_usesUiManagerOverride_whenPresent() {
        Color override = new Color(1, 2, 3);
        UIManager.put("Component.borderColor", override);

        Color c = ThemeAwareColors.getUrgentColor();
        assertNotNull(c);
        assertEquals(override, c);
    }

    @Test
    public void warningColor_usesUiManagerOverride_whenPresent() {
        Color override = new Color(4, 5, 6);
        UIManager.put("Separator.separatorColor", override);

        Color c = ThemeAwareColors.getWarningColor();
        assertNotNull(c);
        assertEquals(override, c);
    }

    @Test
    public void urgentColor_returnsFallback_whenNoUiKeysPresent() {
        assertFallback(ThemeAwareColors.getUrgentColor(), new Color(211, 47, 47), new Color(255, 107, 107));
    }

    @Test
    public void warningColor_returnsFallback_whenNoUiKeysPresent() {
        assertFallback(ThemeAwareColors.getWarningColor(), new Color(255, 160, 0), new Color(255, 200, 100));
    }

    private static void assertFallback(Color color, Color light, Color dark) {
        boolean originallyDark = !JBColor.isBright();
        try {
            JBColor.setDark(false);
            assertEquals(light.getRGB(), color.getRGB());
            JBColor.setDark(true);
            assertEquals(dark.getRGB(), color.getRGB());
        } finally {
            JBColor.setDark(originallyDark);
        }
    }

    @Test
    public void alreadyAssignedColorsFollowUiDefaultsChanges() {
        String[] keys = {"EditorPane.background", "Panel.background", "TextArea.foreground",
                "Separator.separatorColor", "List.selectionBackground", "List.selectionForeground",
                "TextArea.inactiveForeground", "Notifications.successForeground"};
        Object[] previous = java.util.Arrays.stream(keys).map(UIManager::get).toArray();
        try {
            for (String key : keys) UIManager.put(key, new Color(20, 30, 40));
            Color[] colors = {ThemeAwareColors.getBackgroundColor(), ThemeAwareColors.getHeaderColor(),
                    ThemeAwareColors.getTextColor(), ThemeAwareColors.getBorderColor(),
                    ThemeAwareColors.getSelectionColor(), ThemeAwareColors.getSelectionForegroundColor(),
                    ThemeAwareColors.getDisabledTextColor(), ThemeAwareColors.getSuccessColor()};
            JPanel panel = new JPanel();
            panel.setBackground(colors[0]);
            Color next = new Color(190, 200, 210);
            for (String key : keys) UIManager.put(key, next);
            for (Color color : colors) assertEquals(next.getRGB(), color.getRGB());
            assertEquals(next.getRGB(), panel.getBackground().getRGB());
        } finally {
            for (int i = 0; i < keys.length; i++) UIManager.put(keys[i], previous[i]);
        }
    }

    @Test
    public void retainedAccentColorPicksUpNewOverrideAndFallback() {
        Color color = ThemeAwareColors.getImportantBorderColor();
        Color override = new Color(15, 25, 35);
        UIManager.put("Component.borderColor", override);
        assertEquals(override.getRGB(), color.getRGB());
        UIManager.put("Component.borderColor", null);
        assertFallback(color, new Color(241, 90, 35), new Color(255, 140, 70));
    }
}
