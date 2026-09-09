package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@org.junit.jupiter.api.Tag("packs")
class MetaPackSourceValidationTest {
    @Test
    void everyShippedPackPassesStructuralAndReferentialValidation() {
        for (String pack : PackExpectations.metaPacksToTest().toList()) {
            assertDoesNotThrow(() -> ComponentLibrary.refreshComponentLibrary(pack), pack);
        }
    }

    @Test
    void fileTransferConsumersDeclareTheirSyntheticPayloadAdapter() throws StudioBuildException {
        for (String pack : PackExpectations.metaPacksToTest().toList()) {
            assertEquals("ikasan-file-transfer-payload",
                    ComponentLibrary.getIkasanComponentByKey(pack, "FTP Consumer").getTestPayloadAdapter(), pack);
            assertEquals("ikasan-file-transfer-payload",
                    ComponentLibrary.getIkasanComponentByKey(pack, "SFTP Consumer").getTestPayloadAdapter(), pack);
        }
    }

}
