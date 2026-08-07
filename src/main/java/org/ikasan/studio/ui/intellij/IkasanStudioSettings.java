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
}
