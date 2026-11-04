package org.ikasan.studio.testing.packs;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
@org.junit.jupiter.api.Tag("packs")
class FtpFilenameValidationTest {
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void safeMetadataRulePreservesJavaRegexValuesAndLeadingGlobDiagnostics(String pack) throws Exception {
        for (String component : new String[]{"FTP Consumer", "SFTP Consumer"}) {
            var meta = ComponentLibrary.getIkasanComponents(pack).get(component).getMetadata("filenamePattern");
            // These are runtime Java expressions, not meta-pack expressions. Lookaround remains valid here.
            for (String value : new String[]{"", ".*\\.txt", "(?i).*\\.csv", "(?=invoice).*", "(a)\\1", "[a-z]+", "\\*literal"})
                assertTrue(meta.matchesValidation(value), pack + " " + component + " accepts " + value);
            for (String value : new String[]{"*.txt", "+foo", "?file", "file\nname", "file\rname", "file\u0085name", "file\u2028name", "file\u2029name"})
                assertFalse(meta.matchesValidation(value), pack + " " + component + " rejects " + value);
        }
    }
}
