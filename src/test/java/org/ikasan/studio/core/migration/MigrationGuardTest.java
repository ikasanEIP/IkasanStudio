package org.ikasan.studio.core.migration;

import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationGuardTest {
    @Test void migrationAndGenerationExcludeEachOtherWithinOneProject() {
        UiContext first = new UiContext();
        UiContext second = new UiContext();
        assertThat(first.tryBeginGeneration()).isTrue();
        assertThat(first.tryBeginMigration()).isFalse();
        first.endGeneration();
        assertThat(first.tryBeginMigration()).isTrue();
        assertThat(first.tryBeginMigration()).isFalse();
        assertThat(first.tryBeginGeneration()).isFalse();
        assertThat(second.tryBeginGeneration()).isTrue();
        second.endGeneration();
        first.endMigration();
        assertThat(first.tryBeginGeneration()).isTrue();
        first.endGeneration();
    }
}
