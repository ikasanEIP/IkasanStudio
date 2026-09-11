package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
class UserClassGenerationChoiceTest {
    @Test
    void manualUpdatePreservesExistingCodeAndDoesNotCreateARenamedReplacement() throws Exception {
        FlowUserImplementedElement component = FlowUserImplementedElement.flowUserImplementedElementBuilder()
                .componentMeta(TestFixtures.getCustomConverter(BASE_META_PACK).getComponentMeta())
                .componentName("Custom converter").build();
        component.setOverwriteEnabled(false);
        component.setUserClassGenerationDeferred(true);
        assertFalse(component.shouldGenerateUserClass(false));
        assertFalse(component.shouldGenerateUserClass(true));
        component.setOverwriteEnabled(true);
        assertTrue(component.shouldGenerateUserClass(false));
        assertTrue(component.shouldGenerateUserClass(true));
    }

    @Test
    void ordinaryGenerationCreatesMissingStubsButPreservesExistingImplementations() throws Exception {
        FlowUserImplementedElement component = FlowUserImplementedElement.flowUserImplementedElementBuilder()
                .componentMeta(TestFixtures.getCustomConverter(BASE_META_PACK).getComponentMeta())
                .componentName("Custom converter").build();
        component.setOverwriteEnabled(false);
        assertFalse(component.shouldGenerateUserClass(false));
        assertTrue(component.shouldGenerateUserClass(true));
    }
}
