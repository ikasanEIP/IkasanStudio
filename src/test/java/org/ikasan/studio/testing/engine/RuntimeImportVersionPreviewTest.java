package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeImportVersionPreviewTest {
    @Test void readsRuntimeVersionRatherThanApplicationVersion() throws Exception {
        assertEquals("4.1.3", IkasanRuntimeImport.sourceVersion(
                "{\"version\":\"3.1.1-SNAPSHOT\",\"ikasanVersion\":\"4.1.3\",\"flows\":[]}"));
        assertEquals("", IkasanRuntimeImport.sourceVersion("{\"version\":\"3.1.1-SNAPSHOT\",\"flows\":[]}"));
    }

    @Test void usesTheImportersStrictBoundedSingleDocumentParser() {
        for (String invalid : new String[]{"{", "{} {}", "{\"ikasanVersion\":\"4.1.3\",\"ikasanVersion\":\"3.3.9\"}",
                " ".repeat(8 * 1024 * 1024 + 1)}) {
            assertThrows(StudioBuildException.class, () -> IkasanRuntimeImport.sourceVersion(invalid));
        }
    }
}
