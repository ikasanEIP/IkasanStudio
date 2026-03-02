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
        Color c = ThemeAwareColors.getUrgentColor();
        assertNotNull(c);
        assertTrue(c instanceof JBColor);
        JBColor jb = (JBColor) c;
        Color light = new Color(jb.getRed(), jb.getGreen(), jb.getBlue());
        Color dark = jb.getDarkVariant();
        assertEquals(new Color(211, 47, 47), light);
        assertEquals(new Color(255, 107, 107), dark);
    }

    @Test
    public void warningColor_returnsFallback_whenNoUiKeysPresent() {
        Color c = ThemeAwareColors.getWarningColor();
        assertNotNull(c);
        assertTrue(c instanceof JBColor);
        JBColor jb = (JBColor) c;
        Color light = new Color(jb.getRed(), jb.getGreen(), jb.getBlue());
        Color dark = jb.getDarkVariant();
        assertEquals(new Color(255, 160, 0), light);
        assertEquals(new Color(255, 200, 100), dark);
    }
}

