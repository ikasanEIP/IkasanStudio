package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.navigation.NavigationTarget;
import org.ikasan.studio.ui.actions.ComponentNavigationAvailability;
import org.ikasan.studio.ui.actions.NavigateToPropertiesAction;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnsavedPropertyChangesAvailabilityTest {
    @Test void moduleBackgroundHasNoNavigation() {
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class, CALLS_REAL_METHODS);
        doReturn(true).when(panel).dataHasChangedAndOKToProcess();
        try (var cache = mockStatic(ViewHandlerCache.class);
             var dialogs = mockConstruction(UnsavedPropertyChangesDialog.class, (dialog, context) -> {
                 assertNull(context.arguments().get(5));
                 assertNull(context.arguments().get(6));
                 when(dialog.getChoice()).thenReturn(UnsavedPropertyChangesDialog.Choice.CANCEL);
             })) {
            assertFalse(panel.confirmSelectionChangeWithPendingEdits(mock(Module.class)));
            assertEquals(1, dialogs.constructed().size());
            cache.verifyNoInteractions();
            verify(panel, never()).doOKAction();
        }
    }

    @Test void missingTargetsHideButtonsAndPropertiesOnlyTargetNavigatesToClickedComponent() {
        FlowElement clicked = mock(FlowElement.class);
        when(clicked.getIdentity()).thenReturn("Properties only");
        AbstractViewHandlerIntellij handler = mock(AbstractViewHandlerIntellij.class);
        when(handler.getCodeNavigationTarget()).thenReturn(NavigationTarget.none());
        ComponentPropertiesPanel panel = mock(ComponentPropertiesPanel.class, CALLS_REAL_METHODS);
        doReturn(true).when(panel).dataHasChangedAndOKToProcess();
        try (var cache = mockStatic(ViewHandlerCache.class)) {
            assertEquals(new ComponentNavigationAvailability(false, false),
                    ComponentNavigationAvailability.forComponent(null, clicked));
            cache.when(() -> ViewHandlerCache.getAbstractViewHandler(null, clicked)).thenReturn(handler);
            assertEquals(new ComponentNavigationAvailability(false, false),
                    ComponentNavigationAvailability.forComponent(null, clicked));
            when(handler.hasPropertiesNavigationTarget()).thenReturn(true);
            try (var dialogs = mockConstruction(UnsavedPropertyChangesDialog.class, (dialog, context) -> {
                     assertNull(context.arguments().get(5));
                     assertEquals(clicked.getIdentity(), context.arguments().get(6));
                     when(dialog.getChoice()).thenReturn(UnsavedPropertyChangesDialog.Choice.JUMP_TO_PROPERTIES);
                 }); var navigation = mockConstruction(NavigateToPropertiesAction.class, (action, context) ->
                         assertSame(clicked, context.arguments().get(1)))) {
                assertFalse(panel.confirmSelectionChangeWithPendingEdits(clicked));
                assertEquals(1, dialogs.constructed().size());
                verify(navigation.constructed().get(0)).actionPerformed(null);
                verify(panel, never()).doOKAction();
                verify(panel, never()).updateTargetComponent(any());
            }
        }
    }
}
