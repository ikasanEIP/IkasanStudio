package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LaunchBlueActionTest {
    @Test
    void consoleUsesGeneratedContextPathForNamesContainingSpaces() {
        Module module = mock(Module.class);
        when(module.getIdentity()).thenReturn("Order Processing");
        when(module.getPort()).thenReturn("9090");
        assertEquals("http://localhost:9090/order-processing", LaunchBlueAction.consoleUrl(module));
    }

    @Test
    void missingPortUsesTheApplicationDefault() {
        Module module = mock(Module.class);
        when(module.getIdentity()).thenReturn("Orders");
        assertEquals("http://localhost:8080/orders", LaunchBlueAction.consoleUrl(module));
    }
}
