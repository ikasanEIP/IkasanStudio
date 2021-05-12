package org.ikasan.studio;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private static final String HINT_TIPS = "HINT_TIPS";

    private Map<String, Object> applicationOptions;

    public Options() {
        // set defaults
        applicationOptions = new HashMap<>();
        setHintTipsEnabled(Boolean.TRUE);
    }

    public boolean isHintsEnabled() {
        return (Boolean) applicationOptions.get(HINT_TIPS);
    }

    public void setHintTipsEnabled(boolean hintTipsEnabled) {
        applicationOptions.put(HINT_TIPS, hintTipsEnabled);
    }
}
