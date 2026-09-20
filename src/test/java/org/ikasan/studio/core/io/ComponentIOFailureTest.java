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
    void importAcceptsLeadingBomWithoutChangingModelContent() throws Exception {
        Module original = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK,
                Collections.singletonList(TestFixtures.getExceptionResolverFlow(BASE_META_PACK)));
        String json = ComponentIO.toValidatedModuleJson(original);
        Module imported = ComponentIO.validatePersistedModuleJson("\uFEFF" + json, "BOM model.json", false);
        Module withoutBom = ComponentIO.validatePersistedModuleJson(json, "plain model.json", false);
        org.junit.jupiter.api.Assertions.assertEquals(ComponentIO.toValidatedModuleJson(withoutBom),
                ComponentIO.toValidatedModuleJson(imported));
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

    private static String withValueAt(String json, String pointer, String replacementJson) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
        int slash = pointer.lastIndexOf('/');
        com.fasterxml.jackson.databind.JsonNode parent = slash < 0 ? root : root.at("/" + pointer.substring(0, slash));
        String last = pointer.substring(slash + 1);
        com.fasterxml.jackson.databind.JsonNode replacement = mapper.readTree(replacementJson);
        assertFalse(parent.isMissingNode(), "test path does not exist: " + pointer);
        if (parent.isArray()) ((com.fasterxml.jackson.databind.node.ArrayNode) parent).set(Integer.parseInt(last), replacement);
        else ((com.fasterxml.jackson.databind.node.ObjectNode) parent).set(last, replacement);
        return mapper.writeValueAsString(root);
    }

    /**
     * A wrongly typed container used to load "successfully" as an empty design (for example flows: "x" gave a module
     * with no flows), so the next save silently overwrote the model file with the truncated result.
     */
    @Test
    void structurallyDamagedContainersAreRejectedInsteadOfSilentlyDroppingTheDesign() throws Exception {
        Module original = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK,
                Collections.singletonList(TestFixtures.getExceptionResolverFlow(BASE_META_PACK)));
        String json = ComponentIO.toValidatedModuleJson(original);

        for (String[] damage : new String[][]{
                {"flows", "\"x\""}, {"flows", "5"}, {"flows", "{}"},
                {"flows/0", "\"x\""}, {"flows/0", "5"},
                {"flows/0/consumer", "\"x\""}, {"flows/0/consumer", "5"},
                {"flows/0/flowElements", "\"x\""},
                {"flows/0/transitions", "\"x\""},
                {"flows/0/exceptionResolver", "\"x\""}}) {
            String damaged = withValueAt(json, damage[0], damage[1]);
            StudioBuildException failure = assertThrows(StudioBuildException.class,
                    () -> ComponentIO.validatePersistedModuleJson(damaged, "damaged model.json", false),
                    damage[0] + " <- " + damage[1]);
            org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("/" + damage[0]),
                    "the message should name the damaged path: " + failure.getMessage());
        }
    }

    /** These loaded fine but could never be saved again ("The generated JSON failed validation"). */
    @Test
    void anExceptionResolutionThatIsNotAPopulatedObjectIsRejectedOnLoad() throws Exception {
        String json = populatedModelJson();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String key = mapper.readTree(json).at("/flows/0/exceptionResolver").fieldNames().next();
        for (String damage : new String[]{"null", "\"x\"", "5", "[]", "{}"}) {
            com.fasterxml.jackson.databind.node.ObjectNode edited = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
            ((com.fasterxml.jackson.databind.node.ObjectNode) edited.at("/flows/0/exceptionResolver")).set(key, mapper.readTree(damage));
            String damaged = mapper.writeValueAsString(edited);
            StudioBuildException failure = assertThrows(StudioBuildException.class,
                    () -> ComponentIO.validatePersistedModuleJson(damaged, "damaged model.json", false), damage);
            org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains(key), failure.getMessage());
        }
    }

    @Test
    void absentNullAndEmptyContainersStillLoadAsTheyAlwaysDid() throws Exception {
        Module original = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK,
                Collections.singletonList(TestFixtures.getExceptionResolverFlow(BASE_META_PACK)));
        String json = ComponentIO.toValidatedModuleJson(original);

        org.junit.jupiter.api.Assertions.assertNotNull(ComponentIO.validatePersistedModuleJson(json, "as saved", false));
        for (String[] form : new String[][]{
                {"flows", "[]"}, {"flows", "null"}, {"flows/0/consumer", "null"}, {"flows/0/consumer", "{}"},
                {"flows/0/flowElements", "null"}, {"flows/0/flowElements", "[]"},
                {"flows/0/transitions", "null"}, {"flows/0/transitions", "[]"}, {"flows/0/exceptionResolver", "null"}}) {
            String legal = withValueAt(json, form[0], form[1]);
            org.junit.jupiter.api.Assertions.assertNotNull(
                    ComponentIO.validatePersistedModuleJson(legal, "legal model.json", false), form[0] + " <- " + form[1]);
        }
    }

    private static String populatedModelJson() throws Exception {
        try (java.io.InputStream in = ComponentIOFailureTest.class.getResourceAsStream("/org/ikasan/studio/populated_full_module_with_exception_resolver.json")) {
            return new String(java.util.Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * An unexpected failure inside the deserializer (here a component with no name) used to escape as a raw
     * NullPointerException. The plugin's safe-load recovery (preserve the file, disable saves, tell the user) only
     * engages for StudioBuildException, so the model could then be overwritten.
     */
    @Test
    void unexpectedDeserializerFailuresBecomeAStudioBuildException() throws Exception {
        String json = populatedModelJson();
        assertFalse(ComponentIO.validatePersistedModuleJson(json, "as shipped", false).getFlows().isEmpty());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode noName = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
        ((com.fasterxml.jackson.databind.node.ObjectNode) noName.at("/flows/0/consumer")).remove("componentName");
        String damaged = mapper.writeValueAsString(noName);

        StudioBuildException failure = assertThrows(StudioBuildException.class,
                () -> ComponentIO.validatePersistedModuleJson(damaged, "damaged model.json", false));
        org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("damaged model.json"), failure.getMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(failure.getCause(), "the original failure should be kept for diagnosis");
    }

    /** Unquoted numbers are the natural way to hand-edit a port or a numeric name; they used to crash the loader. */
    @Test
    void aNumberWhereTextIsExpectedIsReadAsTextAndSavedAsText() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode edited = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(populatedModelJson());
        edited.put("name", 12345);
        edited.put("port", 8080);

        Module module = ComponentIO.validatePersistedModuleJson(mapper.writeValueAsString(edited), "hand-edited model.json", false);

        org.junit.jupiter.api.Assertions.assertEquals("12345", module.getIdentity());
        org.junit.jupiter.api.Assertions.assertEquals("8080", module.getPort());
        com.fasterxml.jackson.databind.JsonNode saved = mapper.readTree(ComponentIO.toValidatedModuleJson(module));
        org.junit.jupiter.api.Assertions.assertTrue(saved.get("name").isTextual(), "saved as text: " + saved.get("name"));
        org.junit.jupiter.api.Assertions.assertTrue(saved.get("port").isTextual(), "saved as text: " + saved.get("port"));
    }
}
