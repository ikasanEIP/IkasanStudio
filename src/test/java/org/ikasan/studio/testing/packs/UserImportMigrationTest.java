package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.migration.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UserImportMigrationTest {
    @Test void editsOnlyImportsAndPreservesCrLfAndLiterals() {
        String before = """
                /*\r
                import javax.jms.Fake;\r
                */\r
                import javax.jms.Message;\r
                import javax.naming.Context;\r
                import static javax.jms.Session.AUTO_ACKNOWLEDGE;\r
                import javax.jms.*;\r
                class A { javax.jms.Message field; String s = "javax.jms.Message";
                String block = \"""
                import javax.jms.Fake;
                \"""; }""";
        String after = UserImportMigration.rewrite(before, Map.of("javax.jms.", "jakarta.jms."));
        assertEquals(before.replace("import javax.jms.Message;", "import jakarta.jms.Message;")
                .replace("import static javax.jms.Session", "import static jakarta.jms.Session")
                .replace("import javax.jms.*;", "import jakarta.jms.*;"), after);
    }
    @Test void unicodeEscapesAreLeftForReview() {
        String source = "import javax.jms.Message; // " + "\\" + "u000a";
        assertEquals(source, UserImportMigration.rewrite(source, Map.of("javax.jms.", "jakarta.jms.")));
    }
    @Test void bothDirectionalPacksExplicitlyApproveUserImportRules() throws Exception {
        var up = MigrationRules.load("V3.3.9", "V4.1.6");
        assertEquals(new MigrationRules.ImportApplicability(3, 4), up.userImportApplicability());
        var down = MigrationRules.load("V4.1.6", "V3.3.9");
        String source = "import javax.jms.Message;\nimport javax.xml.bind.annotation.XmlRootElement;\n";
        String migrated = UserImportMigration.rewrite(source, up.userImportPrefixes());
        assertTrue(migrated.contains("import jakarta.jms.Message;"));
        assertEquals(source, UserImportMigration.rewrite(migrated, down.userImportPrefixes()));
    }
    @Test void rulesCannotBeAppliedToTheWrongMajorVersionPair() {
        assertThrows(IllegalArgumentException.class, () -> new MigrationRules(1, "V4.1.6", "V3.3.9",
                Map.of(), Map.of(), Map.of("javax.jms.", "jakarta.jms."), new MigrationRules.ImportApplicability(3, 4)));
        assertThrows(IllegalArgumentException.class, () -> new MigrationRules(1, "V3.3.9", "V4.1.6",
                Map.of(), Map.of(), Map.of("javax.jms.", "jakarta.jms."), null));
    }
}
