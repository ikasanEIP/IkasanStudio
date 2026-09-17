package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.util.List;

/** Opens the MCP page without constructing unrelated IDE settings pages. */
final class StudioMcpSettings {
    private static final String ID = "com.intellij.mcpserver.settings";

    private StudioMcpSettings() { }

    static void open(Project project) {
        try {
            Configurable configurable = find(Configurable.APPLICATION_CONFIGURABLE.getExtensionList());
            if (configurable != null) {
                // The platform dialog owns apply/reset/disposal of this configurable.
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
                return;
            }
        } catch (ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (RuntimeException failure) {
            Logger.getInstance(StudioMcpSettings.class).warn("Could not open MCP settings", failure);
        }
        StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("ai.McpSettingsUnavailable"));
    }

    static Configurable find(List<ConfigurableEP<Configurable>> extensions) {
        for (ConfigurableEP<Configurable> extension : extensions) {
            if (ID.equals(extension.id)) return extension.createConfigurable();
        }
        return null;
    }
}
