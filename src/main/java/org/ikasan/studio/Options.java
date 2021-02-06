package org.ikasan.studio;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private static final String HINT_TIPS = "HINT_TIPS";

    private Map<String, Object> options;

    public Options() {
        // set defaults
        options = new HashMap<>();
        setHintTipsEnabled(Boolean.TRUE);
    }

    public boolean isHintsEnabled() {
        return (Boolean)options.get(HINT_TIPS);
    }

    public void setHintTipsEnabled(boolean hintTipsEnabled) {
        options.put(HINT_TIPS, hintTipsEnabled);
    }
}
