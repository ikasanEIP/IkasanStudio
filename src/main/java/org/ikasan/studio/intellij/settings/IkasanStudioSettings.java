package org.ikasan.studio.intellij.settings;

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
    public static final int DEFAULT_COMPONENT_DISTANCE = 30;
    public static final int DEFAULT_FLOW_DISTANCE = 20;
    public static final int MINIMUM_CANVAS_DISTANCE = 0;
    public static final int MAXIMUM_CANVAS_DISTANCE = 250;

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

        /**
         * Periodically poll (every few seconds, in the background) whether a local test mail server is
         * actually listening at each Email Producer's configured address, so the canvas's "Test Mail Server"
         * node/connector lines (see {@code DesignerCanvas#paintTestMailServerNode}) appear and disappear on
         * their own - including if the server is stopped by some means other than the "Stop Test Mail Server"
         * action (e.g. closing its terminal tab directly). On by default, since the check is a near-instant
         * loopback TCP probe in the common case; switch off if mailSmtpHost is ever pointed at something
         * non-local/slow to respond, or in an environment where even loopback connection attempts are
         * intercepted (e.g. some endpoint-security software), where the same probe could block for its full
         * timeout on a background thread every cycle. With this off, the canvas only reflects a one-off check
         * made at the moment "Start Test Mail Server"/"Stop Test Mail Server" is actually clicked - accurate
         * right after a click, but won't notice an externally-stopped server on its own until the next click.
         */
        public boolean testMailServerLivePollingEnabled = true;

        /**
         * While a Studio module process is running, periodically poll (every few seconds, in the background)
         * the module's own Ikasan REST
         * interface for each flow's state, so a flow that has stopped in error (as opposed to a clean stop)
         * flashes red on the canvas without needing to watch the console - see
         * {@code DesignerCanvas#paintFlowErrorFlashes} and {@code FlowErrorMonitorService}. On by default; the
         * check is a single lightweight local HTTP call per tick (no different in kind from the "Send Test
         * Message" or debug-injection calls Studio already makes). No REST calls are made between Studio
         * run/debug sessions. Switch off if this ever proves unwanted background noise - e.g. running many
         * module instances at once.
         */
        public boolean flowErrorMonitoringEnabled = true;

        /** Horizontal distance, in canvas pixels, between ordinary adjacent components. */
        public int componentDistance = DEFAULT_COMPONENT_DISTANCE;

        /** Vertical distance, in canvas pixels, between adjacent flows. */
        public int flowDistance = DEFAULT_FLOW_DISTANCE;
    }

    private State state = new State();

    public static IkasanStudioSettings getInstance() {
        return ApplicationManager.getApplication() != null
                ? ApplicationManager.getApplication().getService(IkasanStudioSettings.class)
                : null;
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

    public static int getComponentDistance() {
        IkasanStudioSettings instance = getInstance();
        State state = instance != null ? instance.getState() : null;
        return normaliseCanvasDistance(state != null ? state.componentDistance : DEFAULT_COMPONENT_DISTANCE,
                DEFAULT_COMPONENT_DISTANCE);
    }

    public static int getFlowDistance() {
        IkasanStudioSettings instance = getInstance();
        State state = instance != null ? instance.getState() : null;
        return normaliseCanvasDistance(state != null ? state.flowDistance : DEFAULT_FLOW_DISTANCE,
                DEFAULT_FLOW_DISTANCE);
    }

    static int normaliseCanvasDistance(int value, int defaultValue) {
        return value < MINIMUM_CANVAS_DISTANCE || value > MAXIMUM_CANVAS_DISTANCE ? defaultValue : value;
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

    // No caller today (IkasanStudioSettingsConfigurable#apply() currently writes the State field directly, like
    // the other settings here) - kept as the public setter symmetric with isShowAdvancedControlsEnabled() and
    // this class's other isX()/setX() pairs, matching setPromptBeforeDeletingUserCode's own real external
    // caller (DeleteComponentAction) as the precedent for why a settings setter earns its place even before a
    // second caller exists.
    @SuppressWarnings("unused")
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

    // No caller today (IkasanStudioSettingsConfigurable#apply() currently writes the State field directly, like
    // the other settings here) - kept as the public setter symmetric with areJmsConnectorsEnabled() and this
    // class's other isX()/setX() pairs, matching setPromptBeforeDeletingUserCode's own real external caller
    // (DeleteComponentAction) as the precedent for why a settings setter earns its place even before a second
    // caller exists.
    @SuppressWarnings("unused")
    public static void setJmsConnectorsEnabled(boolean jmsConnectorsEnabled) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.jmsConnectorsEnabled = jmsConnectorsEnabled;
        }
    }

    public static boolean isTestMailServerLivePollingEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.testMailServerLivePollingEnabled;
    }

    // No caller today (IkasanStudioSettingsConfigurable#apply() currently writes the State field directly, like
    // the other settings here) - kept as the public setter symmetric with isTestMailServerLivePollingEnabled()
    // and this class's other isX()/setX() pairs, matching setPromptBeforeDeletingUserCode's own real external
    // caller (DeleteComponentAction) as the precedent for why a settings setter earns its place even before a
    // second caller exists.
    @SuppressWarnings("unused")
    public static void setTestMailServerLivePollingEnabled(boolean testMailServerLivePollingEnabled) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.testMailServerLivePollingEnabled = testMailServerLivePollingEnabled;
        }
    }

    public static boolean isFlowErrorMonitoringEnabled() {
        IkasanStudioSettings instance = getInstance();
        if (instance == null) return true;
        State s = instance.getState();
        return s == null || s.flowErrorMonitoringEnabled;
    }

    // No caller today (IkasanStudioSettingsConfigurable#apply() currently writes the State field directly, like
    // the other settings here) - kept as the public setter symmetric with isFlowErrorMonitoringEnabled() and
    // this class's other isX()/setX() pairs, matching setPromptBeforeDeletingUserCode's own real external
    // caller (DeleteComponentAction) as the precedent for why a settings setter earns its place even before a
    // second caller exists.
    @SuppressWarnings("unused")
    public static void setFlowErrorMonitoringEnabled(boolean flowErrorMonitoringEnabled) {
        IkasanStudioSettings instance = getInstance();
        State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.flowErrorMonitoringEnabled = flowErrorMonitoringEnabled;
        }
    }
}
