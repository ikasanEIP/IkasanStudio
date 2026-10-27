package org.ikasan.studio.core.persistence.json;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class StudioDocumentMetadataTest {
    @Test void exactPackIdsWinAndCaseOnlyResolutionMustBeUnique() throws Exception {
        assertThat(StudioDocumentMetadata.resolvePackId("v4.1.6", List.of("V4.1.6"))).isEqualTo("V4.1.6");
        assertThat(StudioDocumentMetadata.resolvePackId("v4.1.6", List.of("v4.1.6", "V4.1.6"))).isEqualTo("v4.1.6");
        assertThatThrownBy(() -> StudioDocumentMetadata.resolvePackId("PACK", List.of("Pack", "pack")))
                .hasMessageContaining("Ambiguous meta-pack identifier");
        assertThat(StudioDocumentMetadata.resolvePackId("v4.1.3", List.of("V4.1.6"))).isEqualTo("v4.1.3");
    }
}
