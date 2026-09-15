package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelReferenceReplacementTest {
    private Module module() throws Exception {
        String version = TestFixtures.BASE_META_PACK;
        var consumer = TestFixtures.getSpringJmsConsumer(version);
        Flow flow = TestFixtures.getUnbuiltFlow(version).name("org.example.cat.domain").consumer(consumer).build();
        consumer.setPropertyValue("trustedObjectPackages", "org.example.cat.domain,java.lang");
        var broker = TestFixtures.getBroker(version);
        broker.setContainingFlow(flow);
        broker.setContainingFlowRoute(flow.getFlowRoute());
        broker.setPropertyValue("fromType", "org.example.cat.domain.Order");
        broker.setPropertyValue("toType", "java.util.List<org.example.cat.domain.Order>");
        flow.getFlowRoute().getFlowElements().add(broker);
        return TestFixtures.getMyFirstModuleIkasanModule(version, List.of(flow));
    }

    @Test void previewsClassAndTrustedPackageChangesAndSupportsSelectiveUndoableReplacement() throws Exception {
        Module module = module();
        var changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.debug.domain");
        assertEquals(3, changes.size());
        assertTrue(changes.stream().allMatch(change -> change.property().getValue().equals(change.before())));
        var selected = changes.stream().filter(change -> !change.key().equals("toType")).toList();
        ModelReferenceReplacement.apply(module, selected, true);
        String saved = ModelTemplate.create(module);
        assertTrue(saved.contains("org.example.debug.domain.Order"));
        assertTrue(saved.contains("org.example.debug.domain,java.lang"));
        assertTrue(saved.contains("java.util.List<org.example.cat.domain.Order>"));
        assertEquals("org.example.cat.domain", module.getFlows().get(0).getIdentity());
        ModelReferenceReplacement.apply(module, selected, false);
        assertTrue(selected.stream().allMatch(change -> change.property().getValue().equals(change.before())));
        ModelReferenceReplacement.apply(module, selected, true);
        assertTrue(selected.stream().allMatch(change -> change.property().getValue().equals(change.after())));
    }

    @Test void respectsNameBoundariesAndPreservesListsAndLiteralDollarSigns() throws Exception {
        Module module = module();
        var consumer = module.getFlows().get(0).getConsumer();
        consumer.setPropertyValue("trustedObjectPackages", List.of("org.example.cat.domain", "org.example.cat.domainExtra", "x.org.example.cat.domain", "org.example.cat.domain.sub"));
        var changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.$domain");
        ModelReferenceReplacement.apply(module, changes, true);
        assertEquals(List.of("org.example.$domain", "org.example.cat.domainExtra", "x.org.example.cat.domain", "org.example.$domain.sub"),
                consumer.getPropertyValue("trustedObjectPackages"));
        assertThrows(IllegalArgumentException.class, () -> ModelReferenceReplacement.preview(module, "", "org.example"));
        assertThrows(IllegalArgumentException.class, () -> ModelReferenceReplacement.preview(module, "org.example", ""));
    }

    @Test void stalePreviewAndRemovedComponentsNeverPartiallyApply() throws Exception {
        Module module = module();
        var changes = ModelReferenceReplacement.preview(module, "org.example.cat.domain", "org.example.debug.domain");
        var last = changes.get(changes.size() - 1);
        last.property().setValue("java.lang.String");
        assertThrows(IllegalStateException.class, () -> ModelReferenceReplacement.apply(module, changes, true));
        assertEquals(changes.get(0).before(), changes.get(0).property().getValue());
        last.property().setValue(last.before());
        module.getFlows().get(0).getFlowRoute().getFlowElements().clear();
        assertThrows(IllegalStateException.class, () -> ModelReferenceReplacement.apply(module, changes, true));
        assertEquals(changes.get(0).before(), changes.get(0).property().getValue());
    }
}
