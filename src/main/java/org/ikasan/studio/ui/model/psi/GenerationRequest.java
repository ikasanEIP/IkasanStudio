package org.ikasan.studio.ui.model.psi;

import org.ikasan.studio.core.model.ikasan.instance.Flow;

import java.util.Objects;

/**
 * Describes the smallest safe set of generated artifacts affected by a model mutation.
 * Callers should use {@link #full()} whenever the impact cannot be determined confidently.
 */
public record GenerationRequest(Scope scope, Flow affectedFlow) {
    public enum Scope {
        /** Persist the model only; no code template consumes the changed data. */
        MODEL_ONLY,
        /** Regenerate one flow, its factory/stubs, configuration properties and dependencies. */
        FLOW,
        /** Regenerate module flow registration and optionally one newly added/renamed flow. */
        MODULE_STRUCTURE,
        /** Regenerate every Studio-owned artifact. */
        FULL
    }

    public GenerationRequest {
        Objects.requireNonNull(scope, "scope");
        if (scope == Scope.FLOW && affectedFlow == null) {
            throw new IllegalArgumentException("A flow-scoped generation request requires an affected flow");
        }
    }

    public static GenerationRequest modelOnly() {
        return new GenerationRequest(Scope.MODEL_ONLY, null);
    }

    public static GenerationRequest flow(Flow flow) {
        return new GenerationRequest(Scope.FLOW, Objects.requireNonNull(flow, "flow"));
    }

    public static GenerationRequest moduleStructure(Flow affectedFlow) {
        return new GenerationRequest(Scope.MODULE_STRUCTURE, affectedFlow);
    }

    public static GenerationRequest full() {
        return new GenerationRequest(Scope.FULL, null);
    }
}
