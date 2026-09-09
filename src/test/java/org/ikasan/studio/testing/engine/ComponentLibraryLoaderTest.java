package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.metapack.loading.ComponentLibraryLoader;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("engine")
class ComponentLibraryLoaderTest {
    @Test
    void loadsMetadataWithResourceIdentifiersAndNoUiObjects() {
        var components = new ComponentLibraryLoader().load("TestV1");

        assertThat(components).isNotEmpty();
        assertThat(components.values())
                .allMatch(component -> component.getIconResourceDirectory() != null)
                .allMatch(component -> component.getIconResourceDirectory().startsWith("studio/metapack/"));
    }

}
