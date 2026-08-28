package org.ikasan.studio.ui.actions;

import com.intellij.openapi.ui.Messages;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteComponentActionTest {
    @Test
    void mapsEveryDialogExitToAnExplicitDeletionChoice() {
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.YES))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.DELETE_COMPONENT_AND_CLASS);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.NO))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.DELETE_COMPONENT_ONLY);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.CANCEL))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.CANCEL);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(-1))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.CANCEL);
    }
}
