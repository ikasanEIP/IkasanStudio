package org.ikasan.studio.core.io;

import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComponentIOFailureTest {
    @BeforeAll
    static void loadMetaPack() throws StudioBuildException {
        ComponentLibrary.refreshComponentLibrary(BASE_META_PACK);
    }

    @Test
    void serializationFailureThrowsInsteadOfReturningWritableSentinelText() throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK,
                Collections.singletonList(TestFixtures.getExceptionResolverFlow(BASE_META_PACK)));
        ExceptionResolver resolver = module.getFlows().get(0).getExceptionResolver();
        HashMap<String, ExceptionResolution> damaged = new HashMap<>(resolver.getIkasanExceptionResolutionMap());
        damaged.put("damaged", null);
        resolver.setIkasanExceptionResolutionMap(damaged);

        assertThrows(StudioRuntimeException.class, () -> ComponentIO.toJson(module));
        assertThrows(StudioRuntimeException.class, () -> ComponentIO.toValidatedModuleJson(module));
    }
}
