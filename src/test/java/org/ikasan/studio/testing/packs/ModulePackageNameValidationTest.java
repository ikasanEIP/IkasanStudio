package org.ikasan.studio.testing.packs;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application package name is free text that the generators paste into "package ...;" statements and the
 * component scan, so one that is not a legal Java package (com.my-company.app, com.my company, com.import.x) gives
 * generated code that does not compile, with errors far from the cause. It is validated when the user edits it.
 */
@ExtendWith(SharedResourceExtension.class)
@org.junit.jupiter.api.Tag("packs")
class ModulePackageNameValidationTest {

    private static java.util.regex.Pattern pattern(String pack) throws Exception {
        var meta = ComponentLibrary.getIkasanComponents(pack).get("Module").getMetadata("applicationPackageName");
        assertNotNull(meta.getValidationPattern(), "applicationPackageName should be validated in " + pack);
        assertNotNull(meta.getValidationMessage());
        return meta.getValidationPattern();
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void acceptsLegalJavaPackageNames(String pack) throws Exception {
        var pattern = pattern(pack);
        for (String legal : new String[]{"co.uk.test", "org.ikasan.studio", "com.example.app2", "com.my_company.$app",
                "single", "com.Example.MixedCase", "com.日本.app", "com.imports.x", "com.classes"}) {
            assertTrue(pattern.matcher(legal).matches(), "should accept " + legal);
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void rejectsNamesThatWouldGenerateUncompilableCode(String pack) throws Exception {
        var pattern = pattern(pack);
        for (String illegal : new String[]{"com.my-company.app", "com.my company", "com..x", ".com.x", "com.x.", "1com.x",
                "com.2x", "com.import.x", "default.app", "com.x.class", "com.x.true", "com.x.null", "com.x.", "", "com.a/b"}) {
            assertFalse(pattern.matcher(illegal).matches(), "should reject [" + illegal + "]");
        }
    }
}
