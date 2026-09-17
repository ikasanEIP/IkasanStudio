package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudioMcpSettingsTest {
    @Test void opensOnlyMcpWithoutResolvingUnrelatedSettingsBundles() {
        var unrelated = entry("elevation.settings");
        var mcp = entry("com.intellij.mcpserver.settings");
        var page = mock(Configurable.class);
        when(mcp.createConfigurable()).thenReturn(page);
        assertThat(StudioMcpSettings.find(List.of(unrelated, mcp))).isSameAs(page);
        verifyNoInteractions(unrelated);
        verify(mcp).createConfigurable();
        verifyNoMoreInteractions(mcp);
    }

    @Test void missingMcpDoesNotConstructOtherSettings() {
        var unrelated = entry("elevation.settings");
        assertThat(StudioMcpSettings.find(List.of(unrelated))).isNull();
        verifyNoInteractions(unrelated);
    }

    @SuppressWarnings("unchecked")
    private static ConfigurableEP<Configurable> entry(String id) {
        ConfigurableEP<Configurable> extension = mock(ConfigurableEP.class);
        extension.id = id;
        return extension;
    }
}
