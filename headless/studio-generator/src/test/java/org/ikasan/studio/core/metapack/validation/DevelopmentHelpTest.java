package org.ikasan.studio.core.metapack.validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DevelopmentHelpTest {
    @Test void snapshotsAcceptOnlyFullPinnedCommitsAndReleasesNeverDo() {
        String path = "/ikasanEIP/ikasan/blob/811a14b1ee7c202868d4bfbcac505d5da6a5565e/ikasaneip/Readme.md";
        assertTrue(MetaPackDataValidator.isPinnedDevelopmentHelp("V5.0.1-SNAPSHOT", path));
        assertFalse(MetaPackDataValidator.isPinnedDevelopmentHelp("V5.0.1", path));
        assertFalse(MetaPackDataValidator.isPinnedDevelopmentHelp("V4.1.6", path));
        assertFalse(MetaPackDataValidator.isPinnedDevelopmentHelp("V5.0.1-SNAPSHOT", path.replace("811a14b1ee7c202868d4bfbcac505d5da6a5565e", "main")));
        assertFalse(MetaPackDataValidator.isPinnedDevelopmentHelp("V5.0.1-SNAPSHOT", path.replace("811a14b1ee7c202868d4bfbcac505d5da6a5565e", "811a14b")));
    }
}
