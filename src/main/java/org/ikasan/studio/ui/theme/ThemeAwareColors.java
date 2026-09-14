package org.ikasan.studio.ui.theme;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

/**
 * Centralized theme-aware color utility class.
 * Provides consistent, theme-respecting colors across the entire plugin.
 * <p>Colors use IntelliJ's UIManager with light/dark fallbacks to support:
 * <ul>
 * <li>Light themes (IntelliJ Light, GitHub Light, etc.)</li>
 * <li>Dark themes (Darcula, GitHub Dark, etc.)</li>
 * <li>High contrast themes</li>
 * <li>Custom color schemes</li>
 * </ul>
 * <p>Usage:
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
     * <p>Fallback chain:
     * <ol>
     * <li>EditorPane.background (preferred - matches editor color)</li>
     * <li>Panel.background (fallback - standard panel color)</li>
     * <li>Light/dark panel fallback</li>
     * </ol>
     * <p>This ensures panels blend seamlessly with the IDE's theme.
     * @return a non-null colour that resolves against the current theme when used
     */
    public static Color getBackgroundColor() {
        return pickFromUiKeysOrFallback(new String[]{"EditorPane.background", "Panel.background"}, new JBColor(Gray._255, Gray._60));
    }

    /**
     * Get the border color that respects IntelliJ's current theme.
     * <p>Uses the separator color which is specifically designed to be visible
     * and appropriately styled in any theme.
     * @return the theme-aware separator/border color
     */
    public static Color getBorderColor() {
        return pickFromUiKeysOrFallback(new String[]{"Separator.separatorColor"}, new JBColor(Gray._192, Gray._80));
    }

    /**
     * Get the header background color that respects IntelliJ's current theme.
     * <p>Useful for table headers, section headers, and other UI elements that
     * need a slightly different background shade than regular panels.
     * <p>Fallback chain:
     * <ol>
     * <li>Panel.background (theme-aware panel color)</li>
     * <li>Color.WHITE or Dark Gray (final fallback for regular and dark themes)</li>
     * </ol>
     * @return the theme-aware header background color
     */
    public static Color getHeaderColor() {
        return pickFromUiKeysOrFallback(new String[]{"Panel.background"}, new JBColor(Gray._255, Gray._60));
    }

    /**
     * Get the text foreground color that respects IntelliJ's current theme.
     * <p>Ensures text contrast is appropriate for the current theme.
     * <p>Fallback chain:
     * <ol>
     * <li>TextArea.foreground (text-specific color)</li>
     * <li>Label.foreground (fallback - label color)</li>
     * <li>Color.BLACK or WHITE (final fallback for regular and dark themes)</li>
     * </ol>
     * @return the theme-aware text color
     */
    public static Color getTextColor() {
        return pickFromUiKeysOrFallback(new String[]{"TextArea.foreground", "Label.foreground"}, JBColor.foreground());
    }

    /**
     * Get the selection color that matches the IDE's selection style.
     * <p>Used for highlighting, selection backgrounds, and focus indicators.
     * @return the theme-aware selection background color
     */
    public static Color getSelectionColor() {
        return pickFromUiKeysOrFallback(new String[]{"List.selectionBackground"}, new JBColor(0x3875D6, 0x2F65CA));
    }

    /**
     * Get the selection foreground color that matches the IDE's selection style.
     * <p>Used for text color on selected items to ensure readability.
     * @return the theme-aware selection foreground color
     */
    public static Color getSelectionForegroundColor() {
        return pickFromUiKeysOrFallback(new String[]{"List.selectionForeground"}, new JBColor(Gray._255, Gray._255));
    }

    /**
     * Get the disabled text color for disabled UI components.
     * <p>Ensures disabled text remains readable but visually distinct.
     * Fallback chain:
     * <ol>
     * <li>TextArea.inactiveForeground (text-specific color)</li>
     * <li>Gray or Dark Gray (final fallback for regular and dark themes)</li>
     * </ol>
     * @return the theme-aware disabled text color
     */
    public static Color getDisabledTextColor() {
        return pickFromUiKeysOrFallback(new String[]{"TextArea.inactiveForeground"}, new JBColor(Gray._120, Gray._150));
    }

    /**
     * Check if the current theme is dark.
     * <p>Useful for making theme-specific decisions beyond simple color choices.
     * @return true if dark theme is active, false otherwise
     */
    public static boolean isDarkTheme() {
        Color bg = getBackgroundColor();
        // Simple heuristic: if background is dark, we're in dark theme
        int brightness = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
        return brightness < 128;
    }

    /**
     * Get an "important" border color for emphasizing important sections (e.g. mandatory properties).
     * Tries to use theme-provided colors first, then falls back to a JBColor with light/dark variants.
     * <p>This allows us to draw attention while still respecting the user's theme.
     * <p>Fallback chain:
     * <ol>
     * <li>UIManager key "Component.borderColor" (if present)</li>
     * <li>UIManager key "Separator.separatorColor"</li>
     * <li>Orange or Red/Orange (final fallback for regular and dark themes)</li>
     * </ol>
     */
    public static Color getImportantBorderColor() {
        return pickFromUiKeysOrFallback(new String[]{"Component.borderColor", "Separator.separatorColor"}, new JBColor(0xF15A23, 0xFF8C46));
    }

    /**
     * Pick the first non-null color from UIManager using the supplied keys, or return the given fallback.
     * Callers build that fallback as a {@code new JBColor(light, dark)} literal at the call site (rather than
     * this method taking two separate light/dark {@code Color} params and constructing the JBColor itself) so
     * every raw {@code new Color(...)} in this class stays visibly paired with the JBColor it belongs to, for
     * IntelliJ's own "use JBColor instead of Color" inspection.
     */
    private static Color pickFromUiKeysOrFallback(String[] uiKeys, Color fallback) {
        return JBColor.lazy(() -> {
            for (String key : uiKeys) {
                Color color = UIManager.getColor(key);
                if (color != null) return color;
            }
            return fallback;
        });
    }

    /**
     * Get a theme-aware "urgent/error" color suitable for borders, badges and small accents.
     * <p>Lookup order (first hit returned):
     * <ol>
     * <li>Component.borderColor</li>
     * <li>Separator.separatorColor</li>
     * <li>Notifications.errorForeground</li>
     * <li>Component.focusColor</li>
     * <li>Red or Salmon Pink (final fallback for regular and dark themes)</li>
     * </ol>
     * @return a non-null Color that respects the active theme where possible
     */
    public static Color getUrgentColor() {
        String[] keys = new String[]{
            "Component.borderColor",
            "Separator.separatorColor",
            "Notifications.errorForeground",
            "Component.focusColor"
        };
        return pickFromUiKeysOrFallback(keys, new JBColor(new Color(211, 47, 47), new Color(255, 107, 107)));
    }

    /**
     * Get a theme-aware "warning/attention" color suitable for borders, badges and small accents.
     * <p>Lookup order (first hit returned):
     * <ol>
     * <li>Component.borderColor</li>
     * <li>Separator.separatorColor</li>
     * <li>Notifications.warningForeground</li>
     * <li>Component.focusColor</li>
     * <li>light orange or Fawn (final fallback for regular and dark themes)</li>
     * </ol>
     * @return a non-null Color that respects the active theme where possible
     */
    public static Color getWarningColor() {
        String[] keys = new String[]{
            "Component.borderColor",
            "Separator.separatorColor",
            "Notifications.warningForeground",
            "Component.focusColor"
        };
        return pickFromUiKeysOrFallback(keys, new JBColor(new Color(255, 160, 0), new Color(255, 200, 100)));
    }

    /**
     * Get a theme-aware "success/running" color suitable for borders, badges and small accents.
     * <p>Lookup order (first hit returned):
     * <ol>
     * <li>Notifications.successForeground</li>
     * <li>Green (final fallback for regular and dark themes)</li>
     * </ol>
     * <p>Deliberately does NOT fall through to Component.borderColor/Component.focusColor the way
     * {@link #getUrgentColor()}/{@link #getWarningColor()} do - those two happen to read as red/orange-ish under
     * most themes' border colour, but focusColor is very commonly the theme's blue accent, which would make a
     * "success/go" indicator render blue instead of green (this is exactly what happened to the canvas's Start
     * flow-transport-control button before this fix).
     * @return a non-null Color that respects the active theme where possible
     */
    public static Color getSuccessColor() {
        String[] keys = new String[]{
            "Notifications.successForeground"
        };
        return pickFromUiKeysOrFallback(keys, new JBColor(new Color(46, 125, 50), new Color(106, 191, 105)));
    }
}
