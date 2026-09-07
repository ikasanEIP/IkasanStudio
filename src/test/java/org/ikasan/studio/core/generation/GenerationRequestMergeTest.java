package org.ikasan.studio.core.generation;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GenerationRequestMergeTest {
    @Test
    void propertyBurstStaysPropertyScopedAndNewBatchDoesNotInheritOldWork() {
        UiContext context = new UiContext();
        for (int i = 0; i < 100; i++) context.beginGenerationRequest(new CompletableFuture<>(), GenerationRequest.properties());
        assertEquals(GenerationRequest.properties(), context.getPendingGenerationRequest());
        assertFalse(context.tryBeginMigration());
        for (int i = 0; i < 100; i++) context.endGeneration();
        context.beginGenerationRequest(new CompletableFuture<>(), GenerationRequest.full()); context.endGeneration();
        context.beginGenerationRequest(new CompletableFuture<>(), GenerationRequest.properties());
        assertEquals(GenerationRequest.properties(), context.getPendingGenerationRequest());
        context.endGeneration();
        assertTrue(context.tryBeginMigration()); context.endMigration();
    }

    @Test
    void mergeRetainsAllAffectedFlowsAndModuleChanges() {
        Flow first = mock(Flow.class), second = mock(Flow.class);
        assertEquals(GenerationRequest.flow(first), GenerationRequest.flow(first).merge(GenerationRequest.properties()));
        assertEquals(GenerationRequest.flow(first), GenerationRequest.flow(first).merge(GenerationRequest.flow(first)));
        assertEquals(GenerationRequest.full(), GenerationRequest.flow(first).merge(GenerationRequest.flow(second)));
        assertEquals(GenerationRequest.moduleStructure(first), GenerationRequest.moduleStructure(null).merge(GenerationRequest.flow(first)));
        assertEquals(GenerationRequest.moduleStructure(first), GenerationRequest.flow(first).merge(GenerationRequest.moduleStructure(null)));
        assertEquals(GenerationRequest.full(), GenerationRequest.full().merge(GenerationRequest.properties()));
        assertEquals(GenerationRequest.properties(), GenerationRequest.modelOnly().merge(GenerationRequest.properties()));
    }
}
