package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

/**
 * Application-level persistent settings for Ikasan Studio.
 * Stored in ikasan-studio.xml inside the IDE's config directory.
 */
@State(
    name = "IkasanStudioSettings",
    storages = @Storage("ikasan-studio.xml")
)
public class IkasanStudioSettings implements PersistentStateComponent<IkasanStudioSettings.State> {

    public static class State {
        /** Show contextual instructions while a module or flow is empty. */
        public boolean gettingStartedHintsEnabled = true;

        /** Prompt for confirmation before deleting a component's generated class from the user source tree. */
        public boolean promptBeforeDeletingUserCode = true;

        /**
         * Show rarely-needed controls that most users won't touch day to day - currently just the canvas
         * toolbar's "Load" button (Studio already loads the module automatically on project open; this is
         * only for manually reloading model.json from disk after an external change). Off by default so the
         * common toolbar stays uncluttered; can be switched back on here for the rare case it's needed.
         */
        public boolean showAdvancedControls = false;

        /**
         * Draw a connector line on the canvas between a JMS Producer and a JMS Consumer (in a different flow
         * of the same module) that reference the same destination/connection factory - an ESB-style visual
         * cue for "flow A feeds flow B", without needing a second consuming flow to trace it manually. On by
         * default; can be switched off if it clutters a larger module.
         */
        public boolean jmsConnectorsEnabled = true;
    }

    private State state = new State();

    public static IkasanStudioSettings getInstance() {
        return ApplicationManager.getApplication().getService(IkasanStudioSettings.class);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static boolean areGettingStartedHintsEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.gettingStartedHintsEnabled;
    }

    public static boolean isPromptBeforeDeletingUserCode() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.promptBeforeDeletingUserCode;
    }

    public static void setPromptBeforeDeletingUserCode(boolean promptBeforeDeletingUserCode) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.promptBeforeDeletingUserCode = promptBeforeDeletingUserCode;
        }
    }

    public static boolean isShowAdvancedControlsEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return false;
        State s = instance.getState();
        return s != null && s.showAdvancedControls;
    }

    public static void setShowAdvancedControls(boolean showAdvancedControls) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.showAdvancedControls = showAdvancedControls;
        }
    }

    public static boolean areJmsConnectorsEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.jmsConnectorsEnabled;
    }

    public static void setJmsConnectorsEnabled(boolean jmsConnectorsEnabled) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.jmsConnectorsEnabled = jmsConnectorsEnabled;
        }
    }
}
