package org.ikasan.studio;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private static final String HINT_TIPS = "HINT_TIPS";
    private static final String AUTO_RELOAD_MAVEN = "AUTO_RELOAD_MAVEN";
    private static final String PACKAGE_NAME = "PACKAGE_NAME";

    private final Map<String, Object> applicationOptions;

    public Options() {
        // set defaults
        applicationOptions = new HashMap<>();
        setHintTipsEnabled(Boolean.TRUE);
        setAutoReloadMavenEnabled(Boolean.TRUE);
    }

    /**
     * Generic helper to safely retrieve and cast options stored in the map.
     * Returns the provided defaultValue when the entry is missing or not of the requested type.
     */
    private <T> T getOption(String key, Class<T> type, T defaultValue) {
        Object val = applicationOptions.get(key);
        if (type.isInstance(val)) {
            return type.cast(val);
        }
        return defaultValue;
    }

    public boolean isHintsEnabled() {
        return getOption(HINT_TIPS, Boolean.class, Boolean.FALSE);
    }

    public void setHintTipsEnabled(boolean hintTipsEnabled) {
        applicationOptions.put(HINT_TIPS, hintTipsEnabled);
    }

    public void setPackageName(String packageName) {
        applicationOptions.put(PACKAGE_NAME, packageName);
    }

    public String getPackageName() {
        return getOption(PACKAGE_NAME, String.class, null);
    }

    public boolean isAutoReloadMavenEnabled() {
        return getOption(AUTO_RELOAD_MAVEN, Boolean.class, Boolean.FALSE);
    }

    public void setAutoReloadMavenEnabled(boolean hintTipsEnabled) {
        applicationOptions.put(AUTO_RELOAD_MAVEN, hintTipsEnabled);
    }
}
