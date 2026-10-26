package org.ikasan.studio.intellij.project;

import com.intellij.openapi.components.*;
import org.ikasan.studio.integration.ikasan.RuntimeImportClient;
import org.jetbrains.annotations.NotNull;

/** Last fetch connection, local to this project's IDE workspace. Never stores passwords. */
@Service(Service.Level.PROJECT)
@State(name = "StudioRuntimeImportConnection", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class RuntimeImportConnectionSettings implements PersistentStateComponent<RuntimeImportConnectionSettings.Settings> {
    public static final class Settings {
        public String applicationUrl = "http://localhost:8080/";
        public String moduleName = "";
        public String username = "";
        public boolean includeConfiguration = true;
    }

    private Settings settings = new Settings();

    @Override public Settings getState() { return settings; }

    @Override public void loadState(@NotNull Settings state) {
        settings = new Settings();
        remember(state.applicationUrl, state.moduleName, state.username, state.includeConfiguration);
    }

    /** Invalid URLs never replace the last usable URL or persist embedded credentials. */
    void remember(String url, String module, String username, boolean includeConfiguration) {
        try { settings.applicationUrl = RuntimeImportClient.baseUri(url == null ? "" : url).toString(); }
        catch (IllegalArgumentException invalid) { /* Keep the previous usable URL. */ }
        settings.moduleName = module == null ? "" : module.trim();
        settings.username = username == null ? "" : username;
        settings.includeConfiguration = includeConfiguration;
    }
}
