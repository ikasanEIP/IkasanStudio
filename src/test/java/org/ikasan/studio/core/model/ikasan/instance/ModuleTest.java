package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.TestFixtures.META_IKASAN_PACK_3_3_8;
import static org.ikasan.studio.core.TestFixtures.META_IKASAN_PACK_3_3_3;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.VERSION;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {

    @Test
    void resetMetaPack() throws StudioBuildException {
//        Module module = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, Collections.singletonList(TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK)));
        Module module = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>());
        assertNotNull(module);
        module.setVersion(META_IKASAN_PACK_3_3_3);
//        module.resetMetaPack();

    }
}