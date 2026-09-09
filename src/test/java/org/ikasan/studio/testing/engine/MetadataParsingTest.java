package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("engine")
class MetadataParsingTest {
    @Test
    void duplicateJsonKeysAreRejectedWithAnActionableSource() {
        StudioBuildException exception = assertThrows(StudioBuildException.class,
                () -> ComponentIO.deserializeMetaComponent("studio/validation/duplicate-component-key.json"));

        assertTrue(exception.getMessage().contains("duplicate-component-key.json"));
        assertTrue(exception.getMessage().toLowerCase().contains("duplicate"));
    }
}
