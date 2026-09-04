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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void persistedModelValidationRejectsScalarAndIncompleteConfiguredModels() {
        assertThrows(StudioBuildException.class,
                () -> ComponentIO.validatePersistedModuleJson("\"CouldNotConvert\"", "scalar", true));
        assertThrows(StudioBuildException.class,
                () -> ComponentIO.validatePersistedModuleJson("{\"version\":\"" + BASE_META_PACK + "\"}", "partial", true));
    }

    @Test
    void onlyTheExplicitEmptyArchetypeBootstrapMayBeIncomplete() throws Exception {
        Module bootstrap = ComponentIO.validatePersistedModuleJson("{}", "new project", true);
        assertFalse(bootstrap.isInitialised());
        assertThrows(StudioBuildException.class,
                () -> ComponentIO.validatePersistedModuleJson("{}", "save candidate", false));
    }

    @Test
    void unversionedArchetypeScaffoldingIsAlsoAcceptedAsBootstrap() throws Exception {
        // Real-world shape written by a project archetype before Studio has configured it: some top-level
        // fields already filled in, but no "version" - not a corrupted/partially-saved module.
        String scaffolded = "{\"name\":\"jmsToFtp\",\"applicationPackageName\":\"org.example.jtf\"}";
        Module bootstrap = ComponentIO.validatePersistedModuleJson(scaffolded, "new project", true);
        assertFalse(bootstrap.isInitialised());
        assertThrows(StudioBuildException.class,
                () -> ComponentIO.validatePersistedModuleJson(scaffolded, "save candidate", false));
    }
}
