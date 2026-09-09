package org.ikasan.studio.testkit;

import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Extend in a pack's own JUnit Jupiter project; supply a configured, representative module fixture. */
public abstract class MetaPackContract {
    protected abstract String packId();
    protected abstract Module sampleModule() throws Exception;

    @Test
    void descriptorsSatisfyTheStudioContract() throws Exception {
        MetaPackTestKit.validatePack(packId());
    }

    @Test
    void sampleModelRoundTripsAndRendersDeterministically() throws Exception {
        Module sample = sampleModule();
        assertEquals(packId(), sample.getVersion(), "Sample must target the pack being tested");
        Module restored = MetaPackTestKit.readModel(ModelTemplate.create(sample), "pack contract sample");
        assertEquals(sample.getVersion(), restored.getVersion());
        assertEquals(sample.getPropertyValue("name"), restored.getPropertyValue("name"));
        assertEquals(sample.getFlows().size(), restored.getFlows().size());
        var first = MetaPackTestKit.render(restored);
        assertEquals(first, MetaPackTestKit.render(restored));
        assertTrue(first.values().stream().allMatch(text -> text != null && !text.isBlank()));
    }
}
