package org.ikasan.studio.core.metapack.validation;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaPackSourceValidationTest {
    @Test
    void everyShippedPackPassesStructuralAndReferentialValidation() {
        for (String pack : ComponentLibrary.getMetapackList()) {
            assertDoesNotThrow(() -> ComponentLibrary.refreshComponentLibrary(pack), pack);
        }
    }

    @Test
    void fileTransferConsumersDeclareTheirSyntheticPayloadAdapter() throws StudioBuildException {
        for (String pack : ComponentLibrary.getMetapackList()) {
            assertEquals("ikasan-file-transfer-payload",
                    ComponentLibrary.getIkasanComponentByKey(pack, "FTP Consumer").getTestPayloadAdapter(), pack);
            assertEquals("ikasan-file-transfer-payload",
                    ComponentLibrary.getIkasanComponentByKey(pack, "SFTP Consumer").getTestPayloadAdapter(), pack);
        }
    }

    @Test
    void duplicateJsonKeysAreRejectedWithAnActionableSource() {
        StudioBuildException exception = assertThrows(StudioBuildException.class,
                () -> ComponentIO.deserializeMetaComponent("studio/validation/duplicate-component-key.json"));

        assertTrue(exception.getMessage().contains("duplicate-component-key.json"));
        assertTrue(exception.getMessage().toLowerCase().contains("duplicate"));
    }
}
