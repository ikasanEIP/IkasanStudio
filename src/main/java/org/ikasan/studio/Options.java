package org.ikasan.studio;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private static final String HINT_TIPS = "HINT_TIPS";
    private static final String AUTO_RELOAD_MAVEN = "AUTO_RELOAD_MAVEN";

    private final Map<String, Object> applicationOptions;

    public Options() {
        // set defaults
        applicationOptions = new HashMap<>();
        setHintTipsEnabled(Boolean.TRUE);
        setAutoReloadMavenEnabled(Boolean.TRUE);
    }

    public boolean isHintsEnabled() {
        return (Boolean) applicationOptions.get(HINT_TIPS);
    }

    public void setHintTipsEnabled(boolean hintTipsEnabled) {
        applicationOptions.put(HINT_TIPS, hintTipsEnabled);
    }

    public boolean isAutoReloadMavenEnabled() {
        return (Boolean) applicationOptions.get(AUTO_RELOAD_MAVEN);
    }

    public void setAutoReloadMavenEnabled(boolean hintTipsEnabled) {
        applicationOptions.put(AUTO_RELOAD_MAVEN, hintTipsEnabled);
    }
}
