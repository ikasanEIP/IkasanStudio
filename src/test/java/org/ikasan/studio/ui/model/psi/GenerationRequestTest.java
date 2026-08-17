package org.ikasan.studio.ui.model.psi;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerationRequestTest {
    @BeforeAll
    static void warmUpComponentLibrary() throws StudioBuildException {
        IkasanComponentLibrary.refreshComponentLibrary("V3.3.8");
    }

    @Test
    void modelOnlyDoesNotRequireAnAffectedFlow() {
        GenerationRequest request = GenerationRequest.modelOnly();

        assertEquals(GenerationRequest.Scope.MODEL_ONLY, request.scope());
        assertNull(request.affectedFlow());
    }

    @Test
    void flowScopeRetainsTheAffectedFlow() throws StudioBuildException {
        Flow flow = Flow.flowBuilder().metapackVersion("V3.3.8").build();

        GenerationRequest request = GenerationRequest.flow(flow);

        assertEquals(GenerationRequest.Scope.FLOW, request.scope());
        assertSame(flow, request.affectedFlow());
    }

    @Test
    void flowScopeCannotSilentlyFallBackWithoutAFlow() {
        assertThrows(IllegalArgumentException.class,
                () -> new GenerationRequest(GenerationRequest.Scope.FLOW, null));
    }

    @Test
    void fullScopeRemainsAvailableAsTheConservativeFallback() {
        GenerationRequest request = GenerationRequest.full();

        assertEquals(GenerationRequest.Scope.FULL, request.scope());
        assertNull(request.affectedFlow());
    }
}
