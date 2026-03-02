package org.ikasan.studio.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Centralized theme-aware color utility class.
 * Provides consistent, theme-respecting colors across the entire plugin.
 *
 * All colors are obtained from IntelliJ's UIManager, ensuring they adapt to:
 * - Light themes (IntelliJ Light, GitHub Light, etc.)
 * - Dark themes (Darcula, GitHub Dark, etc.)
 * - High contrast themes
 * - Custom color schemes
 *
 * Usage:
 * <pre>
 *   panel.setBackground(ThemeAwareColors.getBackgroundColor());
 *   panel.setBorder(BorderFactory.createLineBorder(ThemeAwareColors.getBorderColor()));
 * </pre>
 */
public final class ThemeAwareColors {

    // Private constructor - utility class, not to be instantiated
    private ThemeAwareColors() {
        throw new AssertionError("ThemeAwareColors is a utility class and cannot be instantiated");
    }

    /**
     * Get the background color that respects IntelliJ's current theme.
     *
     * Fallback chain:
     * 1. EditorPane.background (preferred - matches editor color)
     * 2. Panel.background (fallback - standard panel color)
     * 3. null (final fallback - Swing uses default)
     *
     * This ensures panels blend seamlessly with the IDE's theme.
     *
     * @return the appropriate background color for the current theme, or null to use defaults
     */
    public static Color getBackgroundColor() {
        Color bg = UIManager.getColor("EditorPane.background");
        if (bg != null) {
            return bg;
        }
        return UIManager.getColor("Panel.background");
    }

    /**
     * Get the border color that respects IntelliJ's current theme.
     *
     * Uses the separator color which is specifically designed to be visible
     * and appropriately styled in any theme.
     *
     * @return the theme-aware separator/border color
     */
    public static Color getBorderColor() {
        return UIManager.getColor("Separator.separatorColor");
    }

    /**
     * Get the header background color that respects IntelliJ's current theme.
     *
     * Useful for table headers, section headers, and other UI elements that
     * need a slightly different background shade than regular panels.
     *
     * Fallback chain:
     * 1. Panel.background (theme-aware panel color)
     * 2. Color.WHITE or Dark Gray (final fallback for regulae and dark themes)
     *
     * @return the theme-aware header background color
     */
    public static Color getHeaderColor() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg != null) {
            return bg;
        }
        // Use JBColor so fallback respects light/dark themes
        return new com.intellij.ui.JBColor(Color.WHITE, new Color(60, 63, 65));
    }

    /**
     * Get the text foreground color that respects IntelliJ's current theme.
     *
     * Ensures text contrast is appropriate for the current theme.
     *
     * Fallback chain:
     * 1. TextArea.foreground (text-specific color)
     * 2. Label.foreground (fallback - label color)
     * 3. Color.BLACK or WHITE (final fallback for regular and dark themes)
     *
     * @return the theme-aware text color
     */
    public static Color getTextColor() {
        Color fg = UIManager.getColor("TextArea.foreground");
        if (fg != null) {
            return fg;
        }
        Color label = UIManager.getColor("Label.foreground");
        if (label != null) {
            return label;
        }
        // Fallback to JBColor that is black on light theme and white on dark theme
        return new com.intellij.ui.JBColor(Color.BLACK, Color.WHITE);
    }

    /**
     * Get the selection color that matches the IDE's selection style.
     *
     * Used for highlighting, selection backgrounds, and focus indicators.
     *
     * @return the theme-aware selection background color
     */
    public static Color getSelectionColor() {
        return UIManager.getColor("List.selectionBackground");
    }

    /**
     * Get the selection foreground color that matches the IDE's selection style.
     *
     * Used for text color on selected items to ensure readability.
     *
     * @return the theme-aware selection foreground color
     */
    public static Color getSelectionForegroundColor() {
        return UIManager.getColor("List.selectionForeground");
    }

    /**
     * Get the disabled text color for disabled UI components.
     *
     * Ensures disabled text remains readable but visually distinct.
     * Fallback chain:
     * 1. TextArea.inactiveForeground (text-specific color)
     * 2. Gray or Dark Gray (final fallback for regular and dark themes)
     *
     * @return the theme-aware disabled text color
     */
    public static Color getDisabledTextColor() {
        Color c = UIManager.getColor("TextArea.inactiveForeground");
        if (c != null) {
            return c;
        }
        return new com.intellij.ui.JBColor(new Color(120, 120, 120), new Color(150, 150, 150));
    }

    /**
     * Check if the current theme is dark.
     *
     * Useful for making theme-specific decisions beyond simple color choices.
     *
     * @return true if dark theme is active, false otherwise
     */
    public static boolean isDarkTheme() {
        Color bg = getBackgroundColor();
        if (bg == null) {
            return false;
        }
        // Simple heuristic: if background is dark, we're in dark theme
        int brightness = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
        return brightness < 128;
    }

    /**
     * Check if high contrast mode is enabled (for accessibility).
     *
     * @return true if high contrast theme is active
     */
    public static boolean isHighContrast() {
        return UIManager.getBoolean("Accessibility.highContrast");
    }

    /**
     * Get an "important" border color for emphasizing important sections (e.g. mandatory properties).
     * Tries to use theme-provided colors first, then falls back to a JBColor with light/dark variants.
     *
     * This allows us to draw attention while still respecting the user's theme.
     *
     * Fallback chain:
     * 1. UIManager key "Component.borderColor" (if present)
     * 2. UIManager key "Separator.separatorColor"
     * 3. Orange or Red/Orange (final fallback for regular and dark themes)
     */
    public static Color getImportantBorderColor() {
        Color ui = UIManager.getColor("Component.borderColor");
        if (ui != null) {
            return ui;
        }
        ui = UIManager.getColor("Separator.separatorColor");
        if (ui != null) {
            return ui;
        }
        return new com.intellij.ui.JBColor(new Color(241, 90, 35), new Color(255, 140, 70));
    }

    /**
     * Pick the first non-null color from UIManager using the supplied keys, or return a JBColor fallback.
     */
    private static Color pickFromUiKeysOrFallback(String[] uiKeys, Color lightFallback, Color darkFallback) {
        for (String key : uiKeys) {
            Color c = UIManager.getColor(key);
            if (c != null) {
                return c;
            }
        }
        return new com.intellij.ui.JBColor(lightFallback, darkFallback);
    }

    /**
     * Get a theme-aware "urgent/error" color suitable for borders, badges and small accents.
     *
     * Lookup order (first hit returned):
     * 1. Component.borderColor
     * 2. Separator.separatorColor
     * 3. Notifications.errorForeground
     * 4. Component.focusColor
     * 5. Red or Salmon Pink (final fallback for regular and dark themes)
     *
     * @return a non-null Color that respects the active theme where possible
     */
    public static Color getUrgentColor() {
        String[] keys = new String[]{
            "Component.borderColor",
            "Separator.separatorColor",
            "Notifications.errorForeground",
            "Component.focusColor"
        };
        return pickFromUiKeysOrFallback(keys, new Color(211, 47, 47), new Color(255, 107, 107));
    }

    /**
     * Get a theme-aware "warning/attention" color suitable for borders, badges and small accents.
     *
     * Lookup order (first hit returned):
     * 1. Component.borderColor
     * 2. Separator.separatorColor
     * 3. Notifications.warningForeground
     * 4. Component.focusColor
     * 5. light orange or Fawn (final fallback for regular and dark themes)
     *
     * @return a non-null Color that respects the active theme where possible
     */
    public static Color getWarningColor() {
        String[] keys = new String[]{
            "Component.borderColor",
            "Separator.separatorColor",
            "Notifications.warningForeground",
            "Component.focusColor"
        };
        return pickFromUiKeysOrFallback(keys, new Color(255, 160, 0), new Color(255, 200, 100));
    }
}
