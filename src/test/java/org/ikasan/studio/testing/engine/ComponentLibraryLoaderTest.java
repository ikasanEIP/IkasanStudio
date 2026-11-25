package org.ikasan.studio.testing.engine;

import java.util.Map;
import org.ikasan.studio.core.metapack.loading.ComponentLibraryLoader;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("engine")
class ComponentLibraryLoaderTest {
    @Test
    void loadsMetadataWithResourceIdentifiersAndNoUiObjects() {
        Map<String,ComponentMeta> components = new ComponentLibraryLoader().load("TestV1");

        assertThat(components).isNotEmpty();
        assertThat(components.values())
                .allMatch(component -> component.getIconResourceDirectory() != null)
                .allMatch(component -> component.getIconResourceDirectory().startsWith("studio/metapack/"));
    }

}
