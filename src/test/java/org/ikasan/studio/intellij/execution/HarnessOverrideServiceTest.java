package org.ikasan.studio.intellij.execution;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HarnessOverrideServiceTest {
    @Test void savedSelectionsPreserveOtherVmOptionsAndCanExplicitlyDisableEveryOverride() {
        String options = "-Xmx1g -Dcustomer.option=retained";
        String selected = HarnessOverrideService.withSelection(options, Set.of("flow.email", "flow.sftp"));
        assertEquals(Set.of("flow.email", "flow.sftp"), HarnessOverrideService.explicitSelection(selected));
        assertTrue(selected.contains("-Xmx1g"));
        assertTrue(selected.contains("-Dcustomer.option=retained"));
        String disabled = HarnessOverrideService.withSelection(selected, Set.of());
        assertEquals(Set.of(), HarnessOverrideService.explicitSelection(disabled));
        assertFalse(disabled.contains("flow.email"));
        assertNull(HarnessOverrideService.explicitSelection(options));
    }
    @Test void differentRunConfigurationsCanRetainDifferentCombinations() {
        String sftp = HarnessOverrideService.withSelection(null, Set.of("flow.sftp"));
        String mail = HarnessOverrideService.withSelection(null, Set.of("flow.mail"));
        assertEquals(Set.of("flow.sftp"), HarnessOverrideService.explicitSelection(sftp));
        assertEquals(Set.of("flow.mail"), HarnessOverrideService.explicitSelection(mail));
    }
}
