package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.GeneratedVerification;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedVerificationTest {
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void rendersCompleteFrozenChecksAndMigrationPreservesModule(String version) throws Exception {
        String model = ModelMigration.analyse(Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson();
        var module = ComponentIO.validatePersistedModuleJson(model, "test", false);
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String application = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var bundle = GeneratedVerification.render(module, model, parent, application);
        assertEquals(parent, bundle.rootPom());
        assertEquals(bundle.rootPom(), GeneratedVerification.render(module, model, bundle.rootPom(), application).rootPom());
        assertEquals(model, bundle.files().get("resources/studio-verification/baseline-model.json"));
        String modelHash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(model.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String support = bundle.files().get("java/org/ikasan/studio/verification/GeneratedVerificationSupport.java");
        assertTrue(support.contains("BASELINE_MODEL_SHA256 = \"" + modelHash + "\";"));
        assertTrue(support.contains("!BASELINE_MODEL_SHA256.equals(hash.toString())"));
        assertTrue(bundle.files().get("resources/studio-verification/baseline.properties").contains("model.sha256=" + modelHash));
        String code = bundle.files().entrySet().stream().filter(e -> e.getKey().endsWith("VerificationTest.java")).findFirst().orElseThrow().getValue();
        assertFalse(code.contains("TODO"));
        assertFalse(code.contains("CONFIGURED"));
        assertFalse(code.contains("testRuntimeBehaviourNotVerified"));
        assertFalse(code.contains("Assume"));
        assertTrue(code.contains("runtime and business scenarios in user-flow-tests"));
        assertTrue(code.contains("assertFactory"));
        assertTrue(bundle.applicationPom().contains("<scope>test</scope>"));
        assertEquals(bundle.applicationPom(), GeneratedVerification.render(module, model, bundle.rootPom(), bundle.applicationPom()).applicationPom());
        var legacy = GeneratedVerification.render(module, model,
                parent.replace("</modules>", "<module>generated-verification</module></modules>"), application);
        assertFalse(legacy.rootPom().contains("<module>generated-verification</module>"));
        var migrated = MigrationArtifacts.render(ModelMigration.analyse(model, version.equals("V3.3.9") ? "V4.1.6" : "V3.3.9"), bundle.rootPom());
        assertFalse(migrated.containsKey("generated/pom.xml"));
        assertTrue(migrated.keySet().stream().noneMatch(p -> p.startsWith("generated/src/test/")));
    }

}
