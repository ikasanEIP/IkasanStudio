package org.ikasan.studio.core.persistence.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.generator.TestUtils;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleDeserializerSafetyTest {
    @BeforeAll
    static void loadMetaPack() throws StudioBuildException {
        ComponentLibrary.refreshComponentLibrary(BASE_META_PACK);
    }

    @Test
    void unknownExceptionActionRejectsTheWholeModel() throws Exception {
        String json = fixture().replaceFirst("\"action\"\\s*:\\s*\"ignore\"", "\"action\":\"notARealAction\"");

        assertThrows(StudioBuildException.class,
                () -> ComponentIO.deserializeModuleInstanceString(json, "unknown-action test"));
    }

    @Test
    void legacyScheduledCronEntryIsMigratedWithoutDroppingData() throws Exception {
        String json = fixture().replace(
                "\"action\": \"retry\",\n          \"actionProperties\": {\n            \"delay\": \"1\",\n            \"interval\": \"2\"\n          }",
                "\"action\": \"scheduledCronEntry\",\n          \"actionProperties\": {\n            \"cronExpression\": \"0 * * * * ? *\",\n            \"maxRetries\": 2\n          }");

        Module module = ComponentIO.deserializeModuleInstanceString(json, "legacy-action test");
        ExceptionResolution resolution = module.getFlows().get(0).getExceptionResolver()
                .getIkasanExceptionResolutionMap().get("javax.jms.JMSException.class");

        assertEquals("scheduledCronRetry", resolution.getTheAction());
        assertEquals("0 * * * * ? *", resolution.getPropertyValue("cronExpression"));
        assertEquals(2, ((Number) resolution.getPropertyValue("maxRetries")).intValue());
    }

    @Test
    void truncatedAndMalformedModelBytesRemainAvailableForRecovery(@TempDir Path directory) throws Exception {
        String valid = fixture();
        for (String damaged : List.of(valid.substring(0, valid.length() / 2), "{\"flows\": [}", "")) {
            Path model = directory.resolve("model.json");
            Files.writeString(model, damaged);
            assertThrows(StudioBuildException.class,
                    () -> ComponentIO.deserializeModuleInstanceString(damaged, "failure injection"));
            assertThrows(IOException.class, () -> ProtectedModelFileWriter.write(model, valid,
                    json -> ComponentIO.deserializeModuleInstanceString(json, "protected write")));
            assertEquals(damaged, Files.readString(model));
        }
    }

    private String fixture() throws Exception {
        return TestUtils.getFileAsString("/org/ikasan/studio/populated_full_module_with_exception_resolver.json");
    }
}
