package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.actions.NavigateToCodeAction;
import org.ikasan.studio.ui.actions.ComponentNavigationAvailability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnsavedPropertyChangesNavigationTest {
    @Test void jumpOpensClickedComponentAndKeepsOriginalSelectionAndEdits() {
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class, CALLS_REAL_METHODS);
        BasicElement original = mock(BasicElement.class);
        BasicElement clicked = mock(BasicElement.class);
        when(original.getIdentity()).thenReturn("Component with pending edits");
        when(clicked.getIdentity()).thenReturn("Component to inspect");
        doReturn(original).when(panel).getSelectedComponent();
        doReturn(true).when(panel).dataHasChangedAndOKToProcess();
        try (var availability = mockStatic(ComponentNavigationAvailability.class);
             var dialogs = mockConstruction(UnsavedPropertyChangesDialog.class, (dialog, context) -> {
            assertEquals(original.getIdentity(), context.arguments().get(3));
            assertEquals(clicked.getIdentity(), context.arguments().get(5));
            when(dialog.getChoice()).thenReturn(UnsavedPropertyChangesDialog.Choice.JUMP_TO_CODE);
        }); var navigation = mockConstruction(NavigateToCodeAction.class, (action, context) -> {
            assertSame(clicked, context.arguments().get(1));
            assertEquals(true, context.arguments().get(2));
        })) {
            availability.when(() -> ComponentNavigationAvailability.forComponent(null, clicked)).thenReturn(new ComponentNavigationAvailability(true, false));
            assertFalse(panel.confirmSelectionChangeWithPendingEdits(clicked));
            assertEquals(1, dialogs.constructed().size());
            assertEquals(1, navigation.constructed().size());
            verify(navigation.constructed().get(0)).actionPerformed(null);
            verify(panel, never()).doOKAction();
            verify(panel, never()).updateTargetComponent(any());
            assertSame(original, panel.getSelectedComponent());
            assertTrue(panel.dataHasChangedAndOKToProcess());
        }
    }

    @Test void launchDialogHasNoClickedTargetAndNavigationChoiceDoesNotApplyEdits() {
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class, CALLS_REAL_METHODS);
        doReturn(true).when(panel).dataHasChangedAndOKToProcess();
        try (var dialogs = mockConstruction(UnsavedPropertyChangesDialog.class, (dialog, context) -> {
            assertNull(context.arguments().get(5));
            assertNull(context.arguments().get(6));
            when(dialog.getChoice()).thenReturn(UnsavedPropertyChangesDialog.Choice.CANCEL);
        }); var navigation = mockConstruction(NavigateToCodeAction.class)) {
            assertNull(panel.preparePendingChangesForLaunch());
            assertEquals(1, dialogs.constructed().size());
            assertTrue(navigation.constructed().isEmpty());
            verify(panel, never()).doOKAction();
        }
        assertEquals(ComponentPropertiesPanel.PendingEditChoice.CANCEL,
                ComponentPropertiesPanel.pendingEditChoice(UnsavedPropertyChangesDialog.Choice.JUMP_TO_CODE));
    }

    @Test void cleanSelectionChangeNeedsNoDialogOrNavigation() {
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class, CALLS_REAL_METHODS);
        try (var dialogs = mockConstruction(UnsavedPropertyChangesDialog.class);
             var navigation = mockConstruction(NavigateToCodeAction.class)) {
            assertTrue(panel.confirmSelectionChangeWithPendingEdits(mock(BasicElement.class)));
            assertTrue(dialogs.constructed().isEmpty());
            assertTrue(navigation.constructed().isEmpty());
        }
    }
}
