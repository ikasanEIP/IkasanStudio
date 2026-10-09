package org.ikasan.studio.ui;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/**
 * Central access point for all user-facing text (button/menu/label captions, tooltips, dialog titles and
 * messages) so the plugin can be localised. Backed by {@code messages/studioBundle.properties} (English, also
 * the fallback for any locale without its own translation) and {@code messages/studioBundle_ja.properties}
 * (Japanese). Add further {@code studioBundle_<language tag>.properties} files for additional languages.
 * <p>
 * Key naming convention: {@code <uiCategory>.<EnglishTextInPascalCase>}, e.g. {@code button.Default} for a
 * button captioned "Default", so the English original is recognisable from the key alone without needing to
 * open the English properties file. See the header comment in {@code studioBundle.properties} for the full
 * list of {@code <uiCategory>} prefixes in use.
 */
public final class StudioBundle {
    @NonNls
    private static final String BUNDLE = "messages.studioBundle";
    private static final DynamicBundle INSTANCE = new DynamicBundle(StudioBundle.class, BUNDLE);

    private StudioBundle() {
    }

    public static @Nls String message(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
