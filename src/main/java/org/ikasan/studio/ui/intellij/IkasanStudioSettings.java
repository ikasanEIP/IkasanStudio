package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.wm.ToolWindowType;
import org.jetbrains.annotations.NotNull;

/**
 * Application-level persistent settings for Ikasan Studio.
 * Stored in ikasan-studio.xml inside the IDE's config directory.
 * Access via IkasanStudioSettings.getInstance().
 */
@State(
    name = "IkasanStudioSettings",
    storages = @Storage("ikasan-studio.xml")
)
public class IkasanStudioSettings implements PersistentStateComponent<IkasanStudioSettings.State> {

    public static class State {
        /** When true (default) the tool window opens in DOCKED mode (shares space with the editor,
         *  drag-and-drop works reliably).
         *  When false it opens in SLIDING mode (overlays the editor, but drag-and-drop
         *  may cause the panel to collapse mid-drag on some platforms). */
        public boolean dockedMode = true;
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

    /**
     * Null-safe read of the docked-mode preference. Returns true (DOCKED) as the safe default
     * if the application service or its state has not yet been initialised.
     */
    public static boolean isDockedModeEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.dockedMode;
    }

    public ToolWindowType getToolWindowType() {
        return isDockedModeEnabled() ? ToolWindowType.DOCKED : ToolWindowType.SLIDING;
    }
}
