package org.ikasan.studio.intellij.project;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationBatchTest {
    @Test
    void preservesOrderAndCoalescesIdenticalWrites() {
        GenerationBatch batch = new GenerationBatch();

        batch.stage("generated/First.java", "first", null);
        batch.stage("generated/Second.java", "second", null);
        batch.stage("generated/First.java", "first", null);

        assertThat(batch.artifacts())
                .extracting(GenerationBatch.Artifact::relativePath)
                .containsExactly("generated/First.java", "generated/Second.java");
    }

    @Test
    void rejectsConflictingOutputForOneTarget() {
        GenerationBatch batch = new GenerationBatch();
        batch.stage("generated/Flow.java", "first", null);

        assertThatThrownBy(() -> batch.stage("generated/Flow.java", "different", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflicting content")
                .hasMessageContaining("generated/Flow.java");
    }

    @Test
    void replacementAuthorityIsExactAndPostCommitWorkIsDeferred() {
        GenerationBatch batch = new GenerationBatch();
        AtomicBoolean callbackRan = new AtomicBoolean();

        batch.authoriseUserReplacement("user/src/main/java/example/Owned.java");
        batch.afterCommit(() -> callbackRan.set(true));

        assertThat(batch.isUserReplacementAuthorised("user/src/main/java/example/Owned.java")).isTrue();
        assertThat(batch.isUserReplacementAuthorised("user/src/main/java/example/Other.java")).isFalse();
        assertThat(callbackRan).isFalse();

        batch.runAfterCommit();
        assertThat(callbackRan).isTrue();
    }
}
