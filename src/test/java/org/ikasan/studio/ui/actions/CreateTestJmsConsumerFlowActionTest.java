package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTestJmsConsumerFlowActionTest {
    /**
     * The harness flow is named from the producer's destination, which is free text. "Test a.b" and "Test a b" are
     * different strings but generate the same Java class, so an exact-text uniqueness check let two flows collide.
     */
    @Test
    void aHarnessFlowNameThatGeneratesTheSameJavaNameAsAnExistingFlowIsMadeUnique() throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        Flow existing = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).build();
        existing.setName("Test a.b");
        module.addFlow(existing);

        assertThat(CreateTestJmsConsumerFlowAction.uniqueFlowName(module, "Test a b")).isEqualTo("Test a b 2");
        assertThat(CreateTestJmsConsumerFlowAction.uniqueFlowName(module, "Test a.b")).isEqualTo("Test a.b 2");
        assertThat(CreateTestJmsConsumerFlowAction.uniqueFlowName(module, "Test other")).isEqualTo("Test other");
    }
}
